package com.onmi.qing.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Qing App 形状系统 - 灵动有机形态
val QingShapes = Shapes(
    // 小型元素：chips、tags、小按钮
    extraSmall = RoundedCornerShape(8.dp),
    // 中小型元素：小卡片、输入框
    small = RoundedCornerShape(16.dp),
    // 中型元素：标准卡片、列表项
    medium = RoundedCornerShape(24.dp),
    // 大型元素：主卡片、底部弹窗
    large = RoundedCornerShape(28.dp),
    // 超大型元素：全屏底部弹窗
    extraLarge = RoundedCornerShape(32.dp)
)

// 独立圆角值 - 用于需要精确控制的场景
object QingCornerRadius {
    // 极小圆角 - 微小组件
    val ExtraSmall = 8.dp
    // 小圆角 - 小型卡片、chips
    val Small = 16.dp
    // 中圆角 - 标准卡片
    val Medium = 24.dp
    // 大圆角 - 大型卡片、按钮
    val Large = 28.dp
    // 特大圆角 - 底部弹窗
    val ExtraLarge = 32.dp

    // 不对称圆角 - 有机形态
    val OrganicTop = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    val OrganicBottom = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 28.dp, bottomEnd = 28.dp)
}