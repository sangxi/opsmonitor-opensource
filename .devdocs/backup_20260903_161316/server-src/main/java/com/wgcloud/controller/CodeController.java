package com.wgcloud.controller;

import com.wgcloud.util.staticvar.StaticKeys;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.security.SecureRandom;

/**
 * 验证码控制器
 * <p>
 * 安全加固：
 * 1. 使用 SecureRandom 替代 Random，防止随机数预测
 * 2. 去除易混淆字符（0/O、1/I/L），提升人工识别准确率
 * 3. 记录生成时间戳到 session，配合 LoginController 做有效期校验
 * 4. 一次性使用：校验后由 LoginController 立即清除 session 中的验证码
 * <p>
 * 体验优化：
 * 1. 图片尺寸 120x40，便于阅读
 * 2. 干扰线降到 40 条，加入噪点 + 字符旋转，兼顾可读性与防识别
 * 3. 字体使用 SansSerif 逻辑字体，Linux/Windows 服务器下均有 fallback
 */
@Controller
@RequestMapping("/code")
public class CodeController {

    private static final int WIDTH = 120;
    private static final int HEIGHT = 40;
    private static final int CODE_COUNT = 4;
    private static final int LINE_COUNT = 40;
    private static final int NOISE_DOT_COUNT = 60;

    /**
     * 验证码字符序列：已去除易混淆字符 0/O、1/I/L
     */
    private static final String[] CODE_SEQUENCE = {
        "A", "B", "C", "D", "E", "F", "G", "H", "J",
        "K", "M", "N", "P", "Q", "R", "S", "T",
        "U", "V", "W", "X", "Y", "Z",
        "2", "3", "4", "5", "6", "7", "8", "9"
    };

    /**
     * 安全随机数生成器（线程安全）
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * 生成验证码
     *
     * @param request
     * @param response
     * @throws IOException
     */
    @RequestMapping("generate")
    @ResponseBody
    public void generateCode(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // 创建图像
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        // 获取图形上下文
        Graphics2D g = image.createGraphics();
        // 设置抗锯齿，提升字符清晰度
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // 设置背景色（浅色，便于阅读）
        g.setColor(new Color(245, 247, 252));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // 绘制边框
        g.setColor(new Color(220, 224, 235));
        g.drawRect(0, 0, WIDTH - 1, HEIGHT - 1);

        // 绘制干扰线（细而浅，不影响阅读）
        for (int i = 0; i < LINE_COUNT; i++) {
            int x = SECURE_RANDOM.nextInt(WIDTH);
            int y = SECURE_RANDOM.nextInt(HEIGHT);
            int xl = SECURE_RANDOM.nextInt(15);
            int yl = SECURE_RANDOM.nextInt(15);
            g.setColor(getRandomColor(160, 210));
            g.drawLine(x, y, x + xl, y + yl);
        }

        // 绘制噪点
        for (int i = 0; i < NOISE_DOT_COUNT; i++) {
            int x = SECURE_RANDOM.nextInt(WIDTH);
            int y = SECURE_RANDOM.nextInt(HEIGHT);
            g.setColor(getRandomColor(140, 200));
            g.fillRect(x, y, 1, 1);
        }

        // 设置字体（SansSerif 为逻辑字体，跨平台 fallback）
        g.setFont(new Font("SansSerif", Font.BOLD, 26));

        // 生成并绘制验证码
        StringBuilder code = new StringBuilder();
        // 字符起始 X 坐标，保证 4 个字符居中
        int startX = 12;
        for (int i = 0; i < CODE_COUNT; i++) {
            String codeStr = CODE_SEQUENCE[SECURE_RANDOM.nextInt(CODE_SEQUENCE.length)];
            code.append(codeStr);
            // 字符随机旋转角度 -25° ~ 25°
            double theta = (SECURE_RANDOM.nextDouble() - 0.5) * Math.PI / 6;
            g.setColor(getRandomColor(30, 130));
            // 字符 Y 坐标轻微抖动，避免完全水平对齐
            int yPos = 28 + SECURE_RANDOM.nextInt(6) - 3;
            // 通过平移+旋转绘制
            g.translate(startX, yPos);
            g.rotate(theta);
            g.drawString(codeStr, 0, 0);
            g.rotate(-theta);
            g.translate(-startX, -yPos);
            // 字符间距 26px，配合 120 宽度
            startX += 26;
        }

        // 将验证码及其生成时间戳存入 session
        HttpSession session = request.getSession();
        session.setAttribute(StaticKeys.SESSION_CODE, code.toString());
        session.setAttribute(StaticKeys.SESSION_CODE_TIME, System.currentTimeMillis());

        // 关闭图形上下文
        g.dispose();

        // 输出图像，禁止缓存
        response.setContentType("image/jpeg");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setDateHeader("Expires", 0);
        OutputStream outputStream = response.getOutputStream();
        ImageIO.write(image, "jpeg", outputStream);
        outputStream.flush();
        outputStream.close();
    }

    /**
     * 获取随机颜色
     *
     * @param min 最小值
     * @param max 最大值
     * @return 颜色
     */
    private Color getRandomColor(int min, int max) {
        if (min > 255) {
            min = 255;
        }
        if (max > 255) {
            max = 255;
        }
        int r = min + SECURE_RANDOM.nextInt(max - min);
        int g = min + SECURE_RANDOM.nextInt(max - min);
        int b = min + SECURE_RANDOM.nextInt(max - min);
        return new Color(r, g, b);
    }
}
