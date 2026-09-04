/**
 * DNShe 免费域名自动续期脚本（青龙面板版）
 *
 * ===== 环境变量 =====
 *   DNSHE_API_KEY      必填。DNShe API Key，形如 cfsd_xxxxxxxxxx
 *   DNSHE_API_SECRET   必填。DNShe API Secret
 *   DNSHE_RENEW_DAYS   可选。剩余多少天内触发续期，默认 30
 *   DNSHE_ONLY_EXPIRED 可选。true=只续期已过期域名；false=剩余≤阈值就续期，默认 false
 *   DNSHE_DEBUG        可选。true=打印详细日志
 *   DNSHE_API_BASE     可选。API 基址，默认 https://api005.dnshe.com/index.php
 *   DNSHE_API_IP       可选。直接指定 IP 绕过 DNS 解析，如 1.2.3.4
 *
 * ===== DNS 解析策略（按优先级）=====
 *   1. DNSHE_API_IP 环境变量 — 手动指定 IP，完全绕过 DNS
 *   2. DNS-over-HTTPS (Google) — 使用 443 端口，不受 UDP 53 封锁影响
 *   3. 系统 DNS — 默认方式，若服务器 UDP 53 被封会失败
 *
 * ===== 安装依赖 =====
   青龙面板 → 依赖管理 → NodeJs 依赖 → 新建 → 填 axios
 *
 * ===== 添加任务 =====
   青龙面板 → 定时任务 → 新建
     名称: DNShe 域名续期
     命令: task dnshe_renew.js
     定时: 30 8 * * *        （每天 8:30 执行）
 *
 * ===== 通知 =====
   通过青龙的 sendNotify 模块发送
 */

const axios = require('axios');
const { URL } = require('url');
const https = require('https');

// ============ 配置 ============
const API_BASE = process.env.DNSHE_API_BASE || 'https://api005.dnshe.com/index.php';
const API_KEY = process.env.DNSHE_API_KEY || '';
const API_SECRET = process.env.DNSHE_API_SECRET || '';
const DNSHE_API_IP = process.env.DNSHE_API_IP || '';
const RENEW_DAYS = parseInt(process.env.DNSHE_RENEW_DAYS || '30', 10);
const ONLY_EXPIRED = process.env.DNSHE_ONLY_EXPIRED === 'true';
const DEBUG = process.env.DNSHE_DEBUG === 'true';

const REQUEST_INTERVAL_MS = 1200;
const REQUEST_TIMEOUT = 20000;
const MAX_RETRIES = 3;

// DNS-over-HTTPS 服务
const DOH_URLS = [
    'https://dns.google/dns-query',
    'https://cloudflare-dns.com/dns-query',
    'https://dns.alidns.com/dns-query',  // 阿里云公共 DNS，国内推荐
];

// 要解析的目标域名
const TARGET_HOST = 'api005.dnshe.com';

const headers = {
    'X-API-Key': API_KEY,
    'X-API-Secret': API_SECRET,
    'Content-Type': 'application/json',
};

// 通知模块
let notify = null;
try { notify = require('./sendNotify'); } catch (e1) {
    try { notify = require('sendNotify'); } catch (e2) {
        debug('未找到 sendNotify 模块，跳过通知');
    }
}

// ============ 工具函数 ============
function log(...args) {
    const now = new Date().toLocaleString('zh-CN', { timeZone: 'Asia/Shanghai' });
    console.log(`[${now}]`, ...args);
}

function debug(...args) {
    if (DEBUG) console.log('[DEBUG]', ...args);
}

function sleep(ms) {
    return new Promise(r => setTimeout(r, ms));
}

async function sendNotify(title, content) {
    if (notify && typeof notify.sendNotify === 'function') {
        try {
            await notify.sendNotify(title, content);
            log('📧 通知已发送:', title);
        } catch (e) {
            log('⚠️ 通知发送失败:', e.message);
        }
    }
}

// ============ DNS 解析 ============
// 方式 1: DNS-over-HTTPS（443 端口，不受 UDP 53 封锁影响）
async function resolveViaDoH(dohUrl, hostname) {
    return new Promise((resolve) => {
        const url = `${dohUrl}?name=${hostname}&type=A`;
        const options = {
            method: 'GET',
            hostname: new URL(dohUrl).hostname,
            path: `/dns-query?name=${hostname}&type=A`,
            headers: {
                'Accept': 'application/dns-json',
                'User-Agent': 'Mozilla/5.0',
            },
            timeout: 8000,
        };

        const req = https.request(options, (res) => {
            let data = '';
            res.on('data', (chunk) => { data += chunk; });
            res.on('end', () => {
                try {
                    const json = JSON.parse(data);
                    const answers = json.Answer || [];
                    const ipv4 = answers.find(a => a.type === 1); // type 1 = A
                    if (ipv4 && ipv4.data) {
                        resolve(ipv4.data);
                    } else {
                        resolve(null);
                    }
                } catch (e) {
                    resolve(null);
                }
            });
        });

        req.on('error', () => resolve(null));
        req.on('timeout', () => { req.destroy(); resolve(null); });
        req.end();
    });
}

// 解析域名（按优先级：手动 IP → DoH → 系统 DNS）
let resolvedIp = null;

async function resolveHostIp() {
    // 1. 手动指定 IP
    if (DNSHE_API_IP) {
        log('🔍 使用手动指定 IP:', DNSHE_API_IP);
        resolvedIp = DNSHE_API_IP;
        return resolvedIp;
    }

    // 2. DNS-over-HTTPS
    for (const doh of DOH_URLS) {
        const provider = doh.includes('google') ? 'Google'
            : doh.includes('cloudflare') ? 'Cloudflare'
            : '阿里云';
        debug(`🔍 尝试 ${provider} DoH: ${doh}`);
        const ip = await resolveViaDoH(doh, TARGET_HOST);
        if (ip) {
            log(`✅ DoH (${provider}) 解析成功: ${TARGET_HOST} → ${ip}`);
            resolvedIp = ip;
            return resolvedIp;
        }
        debug(`  ${provider} DoH 解析失败`);
    }

    // 3. 系统 DNS 兜底
    debug('🔍 尝试系统 DNS 解析');
    try {
        const dns = require('dns');
        const result = await new Promise((resolve, reject) => {
            dns.resolve4(TARGET_HOST, (err, addresses) => {
                if (err) reject(err);
                else resolve(addresses[0] || null);
            });
        });
        if (result) {
            log(`✅ 系统 DNS 解析成功: ${TARGET_HOST} → ${result}`);
            resolvedIp = result;
            return resolvedIp;
        }
    } catch (e) {
        debug('  系统 DNS 解析失败:', e.message);
    }

    log('❌ 所有 DNS 解析方式均失败');
    return null;
}

// ============ URL 构建 ============
function buildUrl(endpoint, action, extraParams = {}) {
    const url = new URL(API_BASE);
    url.searchParams.set('m', 'domain_hub');
    url.searchParams.set('endpoint', endpoint);
    url.searchParams.set('action', action);
    for (const [k, v] of Object.entries(extraParams)) {
        url.searchParams.set(k, String(v));
    }
    return url.toString();
}

// 构建自定义 axios config（指定 IP 时用 Host 头）
function buildAxiosConfig() {
    if (!resolvedIp) return {};

    const baseUrlObj = new URL(API_BASE);
    const host = baseUrlObj.host; // api005.dnshe.com
    const protocol = baseUrlObj.protocol; // https:
    const port = baseUrlObj.port || (protocol === 'https:' ? 443 : 80);

    return {
        // 用 IP 直接连，绕过 DNS
        baseURL: `${protocol}//${resolvedIp}:${port}`,
        headers: {
            Host: host,
        },
    };
}

// ============ HTTP 请求 ============
async function requestWithRetry(method, endpoint, action, data = null, extraParams = {}) {
    const url = buildUrl(endpoint, action, extraParams);
    debug(`${method}`, url);

    let lastError = null;
    for (let attempt = 1; attempt <= MAX_RETRIES; attempt++) {
        try {
            const opts = {
                headers,
                timeout: REQUEST_TIMEOUT,
                maxRedirects: 5,
            };

            // 如果有解析好的 IP，加自定义 axios 配置
            if (resolvedIp) {
                const ipConfig = buildAxiosConfig();
                Object.assign(opts, ipConfig);
                // 覆盖 URL 的 host 为 IP
                const urlObj = new URL(url);
                urlObj.hostname = resolvedIp;
                const finalUrl = urlObj.toString();
                debug(`  → 请求实际地址: ${finalUrl}`);

                let res;
                if (method === 'GET') {
                    res = await axios.get(finalUrl, opts);
                } else {
                    res = await axios.post(finalUrl, data, opts);
                }
                return { success: true, data: res.data };
            }

            let res;
            if (method === 'GET') {
                res = await axios.get(url, opts);
            } else {
                res = await axios.post(url, data, opts);
            }
            return { success: true, data: res.data };
        } catch (e) {
            lastError = e;
            const resp = e.response;
            const errCode = resp && resp.data ? resp.data.error_code : '';
            const errMsg = e.message;

            // 网络错误重试
            const isNetworkError = !resp || resp.status === undefined
                || errMsg.includes('EAI_AGAIN')
                || errMsg.includes('ETIMEDOUT')
                || errMsg.includes('ECONNREFUSED')
                || errMsg.includes('ECONNRESET')
                || errMsg.includes('socket hang up')
                || errMsg.includes('ENOTFOUND')
                || errMsg.includes('getaddrinfo');

            if (isNetworkError && attempt < MAX_RETRIES) {
                const waitMs = 2 ** attempt * 1000;
                log(`⚠️ 网络错误 (第${attempt}次)，${waitMs/1000}秒后重试: ${errMsg}`);

                // 如果解析失败，重试时尝试重新解析
                if (!resolvedIp) {
                    debug('  尝试重新解析 DNS...');
                    resolvedIp = await resolveHostIp();
                }

                await sleep(waitMs);
                continue;
            }

            // API 错误直接返回
            const apiErrorMsg = resp && resp.data
                ? (resp.data.message || resp.data.error || errMsg)
                : errMsg;

            return {
                success: false,
                status: resp ? resp.status : null,
                data: resp ? resp.data : null,
                error: apiErrorMsg,
                error_code: errCode,
            };
        }
    }

    return {
        success: false,
        status: null,
        data: null,
        error: lastError.message,
        error_code: '',
    };
}

// ============ API 调用 ============
async function listAllSubdomains() {
    const all = [];
    let page = 1;
    const perPage = 100;
    let hasMore = true;

    while (hasMore) {
        const r = await requestWithRetry('GET', 'subdomains', 'list', null, {
            page, per_page: perPage, include_total: 1,
            fields: 'id,subdomain,rootdomain,full_domain,status,expires_at,never_expires',
        });

        if (!r.success) {
            throw new Error(`List 失败: ${r.error_code ? `[${r.error_code}] ` : ''}${r.error}`);
        }

        all.push(...((r.data || {}).subdomains || []));
        debug(`第 ${page} 页获取 ${r.data.count} 条，累计 ${all.length} 条`);

        const p = (r.data || {}).pagination;
        if (p && p.has_more) {
            page = p.next_page;
            await sleep(REQUEST_INTERVAL_MS);
        } else {
            hasMore = false;
        }
    }

    return all;
}

async function renewSubdomain(id) {
    return requestWithRetry('POST', 'subdomains', 'renew', { subdomain_id: id });
}

function getRemainingDays(expiresAt) {
    if (!expiresAt) return null;
    const exp = new Date(String(expiresAt).replace(' ', 'T'));
    if (isNaN(exp.getTime())) return null;
    const now = new Date();
    return Math.floor((exp - now) / (1000 * 60 * 60 * 24));
}

// ============ 主流程 ============
(async () => {
    log('🚀 DNShe 域名续期任务开始');
    log(`配置: 触发阈值 = 剩余 ≤ ${RENEW_DAYS} 天，仅续期已过期 = ${ONLY_EXPIRED}`);

    // 1. 参数校验
    if (!API_KEY || !API_SECRET) {
        const msg = '未配置 DNSHE_API_KEY 或 DNSHE_API_SECRET 环境变量';
        log('❌', msg);
        await sendNotify('DNShe 续期失败', msg);
        return;
    }

    // 2. DNS 解析（优先 DoH）
    resolvedIp = await resolveHostIp();
    if (!resolvedIp) {
        const msg = `DNS 解析失败，无法连接 ${TARGET_HOST}。请检查：
  1. 服务器网络是否能访问外网
  2. 是否设置了 DNSHE_API_IP 手动指定 IP
  3. 青龙容器是否配置了正确的网络`;
        log('❌', msg);
        await sendNotify('DNShe 续期失败', msg);
        return;
    }

    // 3. 拉取所有子域名
    let subdomains;
    try {
        subdomains = await listAllSubdomains();
    } catch (e) {
        const msg = `获取子域名列表失败: ${e.message}`;
        log('❌', msg);
        await sendNotify('DNShe 续期失败', msg);
        return;
    }

    log(`✅ 共获取 ${subdomains.length} 个子域名`);

    // 4. 筛选需要续期
    const toRenew = [];
    for (const d of subdomains) {
        if (d.never_expires === 1 || d.never_expires === true || d.never_expires === '1') {
            debug(`跳过[永不过期] ${d.full_domain}`);
            continue;
        }

        const days = getRemainingDays(d.expires_at);
        if (days === null) {
            debug(`跳过[无过期时间] ${d.full_domain}`);
            continue;
        }

        const needRenew = ONLY_EXPIRED ? (days < 0) : (days <= RENEW_DAYS);

        if (needRenew) {
            toRenew.push({ ...d, remaining_days: days });
        } else {
            debug(`跳过[剩余 ${days} 天] ${d.full_domain}`);
        }
    }

    log(`📋 待续期: ${toRenew.length} 个`);
    toRenew.forEach(d => {
        const flag = d.remaining_days < 0 ? '已过期' : `剩余 ${d.remaining_days} 天`;
        log(`  - ${d.full_domain} (ID:${d.id}, 状态:${d.status}, ${flag})`);
    });

    if (toRenew.length === 0) {
        log('✨ 无需续期，结束');
        await sendNotify('DNShe 续期报告', `共 ${subdomains.length} 个子域名，无需续期`);
        return;
    }

    // 5. 执行续期
    const results = [];
    let successCount = 0;
    let failCount = 0;
    let totalCharged = 0;

    for (const d of toRenew) {
        log(`🔄 续期中: ${d.full_domain} (ID:${d.id})`);
        const r = await renewSubdomain(d.id);
        results.push({ domain: d.full_domain, id: d.id, status: d.status, remaining: d.remaining_days, result: r });

        if (r.success) {
            successCount++;
            const data = r.data || {};
            const newExp = data.new_expires_at || '?';
            const charged = parseFloat(data.charged_amount || 0);
            totalCharged += charged;
            log(`  ✅ 成功: 新过期 ${newExp}, 扣费 ${charged}`);
        } else {
            failCount++;
            const errData = r.data || {};
            const errMsg = errData.message || errData.error || r.error || 'unknown';
            const errCode = errData.error_code || r.error_code || '';

            if (errCode === 'renewal_not_yet_available') {
                log(`  ⏳ 还未进入续期窗口。错误: ${errMsg}`);
            } else if (errCode === 'insufficient_balance_for_redemption_renewal') {
                log(`  💰 赎回期余额不足。错误: ${errMsg}`);
            } else {
                log(`  ❌ 失败: [HTTP ${r.status}] ${errCode} - ${errMsg}`);
            }
        }

        await sleep(REQUEST_INTERVAL_MS);
    }

    // 6. 汇总 + 通知
    log('\n========== 续期汇总 ==========');
    log(`待续期: ${toRenew.length}`);
    log(`成功: ${successCount}`);
    log(`失败: ${failCount}`);
    log(`总扣费: ${totalCharged}`);

    let content = '';
    content += `待续期: ${toRenew.length} 个\n`;
    content += `成功: ${successCount} | 失败: ${failCount}\n`;
    content += `总扣费: ${totalCharged}\n\n`;

    for (const r of results) {
        if (r.result.success) {
            const d = r.result.data || {};
            content += `✅ ${r.domain}\n`;
            content += `   原过期: ${d.previous_expires_at || '?'}\n`;
            content += `   新过期: ${d.new_expires_at || '?'}\n`;
            content += `   扣费: ${d.charged_amount || 0}\n\n`;
        } else {
            const ed = r.result.data || {};
            const errMsg = ed.message || ed.error || r.result.error;
            content += `❌ ${r.domain}\n`;
            content += `   错误: [${r.result.status}] ${ed.error_code || ''} ${errMsg}\n\n`;
        }
    }

    const title = `DNShe 续期报告 (✅${successCount} ❌${failCount})`;
    await sendNotify(title, content);

    log('🎉 续期任务完成');
})().catch(e => {
    log('💥 未捕获异常:', e.message);
    if (notify && typeof notify.sendNotify === 'function') {
        notify.sendNotify('DNShe 续期异常', `未捕获异常: ${e.message}\n${e.stack || ''}`);
    }
    process.exit(1);
});
