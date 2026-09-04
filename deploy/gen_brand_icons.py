# -*- coding: utf-8 -*-
"""
OpsMonitor 品牌图标生成器
生成: favicon.png / logo.png (static/logincss) 和 AdminLTELogo.png (AdminLTE/dist/img)
设计: 深色紫蓝渐变圆角底 + 白色监控脉冲(ECG)波形 + 微光
"""
import os
import math
from PIL import Image, ImageDraw, ImageFilter

# ---- 品牌色 ----
C_INDIGO = (99, 102, 241)    # #6366f1
C_PURPLE = (139, 92, 246)    # #8b5cf6
C_DEEP   = (15, 23, 42)      # 深色描边/阴影
C_WHITE  = (255, 255, 255)
C_CYAN   = (165, 180, 252)   # 浅紫蓝光

SS = 4  # 超采样倍数, 抗锯齿


def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def vertical_gradient(size, top, bottom):
    """对角渐变"""
    img = Image.new("RGB", (size, size), top)
    px = img.load()
    for y in range(size):
        for x in range(size):
            t = (x / size) * 0.55 + (y / size) * 0.45
            px[x, y] = lerp(top, bottom, t)
    return img


def rounded_mask(size, radius):
    mask = Image.new("L", (size, size), 0)
    d = ImageDraw.Draw(mask)
    d.rounded_rectangle([0, 0, size - 1, size - 1], radius=radius, fill=255)
    return mask


def draw_pulse(draw, size, color, width):
    """在中心画一条 ECG 监控脉冲波形"""
    cy = size * 0.5
    # 波形关键点 (相对坐标), 经典心跳/监控波形
    pts = [
        (0.00, 0.50),
        (0.18, 0.50),
        (0.26, 0.50),
        (0.32, 0.30),   # 小上峰
        (0.38, 0.62),   # 下探
        (0.46, 0.16),   # 主尖峰
        (0.54, 0.82),   # 深谷
        (0.62, 0.50),
        (0.70, 0.40),   # 次小峰
        (0.78, 0.50),
        (1.00, 0.50),
    ]
    coords = []
    for (x, y) in pts:
        # 左右各留 8% 边距, 避免横线顶到圆角边缘
        px = (0.08 + x * 0.84) * size
        py = y * size
        coords.append((int(px), int(py)))
    draw.line(coords, fill=color, width=width, joint="curve")
    # 主尖峰处加一个光点
    lx, ly = int((0.08 + 0.46 * 0.84) * size), int(0.16 * size)
    r = width * 1.6
    draw.ellipse([lx - r, ly - r, lx + r, ly + r], fill=color)


def make_icon(out_size, rounded=True):
    s = out_size * SS
    # 1. 渐变底 (RGBA)
    icon = vertical_gradient(s, C_INDIGO, C_PURPLE).convert("RGBA")

    # 2. 顶部高光 (alpha 合成, 不破坏底色)
    glow = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    gd = ImageDraw.Draw(glow)
    gd.ellipse([int(-s*0.2), int(-s*0.30), int(s*1.2), int(s*0.55)],
               fill=(255, 255, 255, 34))
    icon.alpha_composite(glow)

    # 3. 脉冲波形 (柔光层 + 实线层)
    wave_layer = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    wd = ImageDraw.Draw(wave_layer)
    w = max(3, int(s * 0.060))
    draw_pulse(wd, s, (255, 255, 255, 255), w)
    glow2 = wave_layer.filter(ImageFilter.GaussianBlur(radius=s * 0.014))
    icon.alpha_composite(glow2)
    icon.alpha_composite(wave_layer)

    # 4. 最后统一应用圆角遮罩
    if rounded:
        radius = int(s * 0.24)
        mask = rounded_mask(s, radius)
        icon.putalpha(mask)

    # 缩放到目标尺寸
    icon = icon.resize((out_size, out_size), Image.LANCZOS)
    return icon


def main():
    root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    logincss = os.path.join(root, "opsmonitor-server", "src", "main", "resources",
                            "static", "logincss")
    adminlte_img = os.path.join(root, "opsmonitor-server", "src", "main", "resources",
                                "static", "AdminLTE", "dist", "img")

    targets = [
        (os.path.join(logincss, "favicon.png"), 128),
        (os.path.join(logincss, "logo.png"), 200),
        (os.path.join(adminlte_img, "AdminLTELogo.png"), 200),
    ]
    for path, size in targets:
        icon = make_icon(size, rounded=True)
        icon.save(path, "PNG")
        print("saved:", path, size, "x", size)


if __name__ == "__main__":
    main()
