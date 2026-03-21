package com.example.demo002

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri

// ══════════════════════════════════════════════
//  数据模型
// ══════════════════════════════════════════════

private data class ContactCard(
    val icon       : ImageVector,
    val iconColor  : Color,
    val title      : String,
    val subtitle   : String,
    val actionLabel: String
)

private const val CONTACT_EMAIL = "3242752034@qq.com"

private val contactCards = listOf(
    ContactCard(
        icon        = Icons.Rounded.Star,
        iconColor   = Color(0xFFFFB347),
        title       = "提出建议",
        subtitle    = "有更好的功能想法？告诉我！",
        actionLabel = "发送建议"
    ),
    ContactCard(
        icon        = Icons.Rounded.Favorite,
        iconColor   = Color(0xFFFB7185),
        title       = "给点鼓励",
        subtitle    = "你的支持是我继续开发的动力 ❤️",
        actionLabel = "送出鼓励"
    ),
    ContactCard(
        icon        = Icons.Rounded.Warning,
        iconColor   = Color(0xFF34D399),
        title       = "反馈 Bug",
        subtitle    = "发现问题？帮我一起让它更好用",
        actionLabel = "报告问题"
    )
)

// ══════════════════════════════════════════════
//  发送邮件工具函数
// ══════════════════════════════════════════════

private fun sendEmail(context: Context, subject: String) {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = "mailto:$CONTACT_EMAIL".toUri()
        putExtra(Intent.EXTRA_EMAIL, arrayOf(CONTACT_EMAIL))
        putExtra(Intent.EXTRA_SUBJECT, subject)
    }
    try {
        context.startActivity(Intent.createChooser(intent, "选择邮件应用"))
    } catch (e: Exception) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("email", CONTACT_EMAIL))
        Toast.makeText(context, "邮箱已复制：$CONTACT_EMAIL", Toast.LENGTH_LONG).show()
    }
}

// ══════════════════════════════════════════════
//  主界面
// ══════════════════════════════════════════════

@Composable
fun HelpDeveloper(
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        MyFirstScreen()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            HelpTopBar()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(8.dp))
                DeveloperAvatar()
                Spacer(Modifier.height(32.dp))
                SectionTitle("我能帮你什么？")
                Spacer(Modifier.height(12.dp))
                contactCards.forEach { card ->
                    ContactReasonCard(card = card, context = context)
                    Spacer(Modifier.height(12.dp))
                }
                Spacer(Modifier.height(28.dp))
                FooterNote()
                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

// ══════════════════════════════════════════════
//  顶部栏（无返回键）
// ══════════════════════════════════════════════

@Composable
private fun HelpTopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                "联系作者",
                style = TextStyle(
                    fontSize   = 22.sp,
                    fontWeight = FontWeight.Black,
                    color      = Color(0xFF1C1C1E)
                )
            )
            Text(
                "有任何想法都可以告诉我",
                style = TextStyle(fontSize = 12.sp, color = Color(0xFF94A3B8))
            )
        }
    }
}

// ══════════════════════════════════════════════
//  开发者头像区
// ══════════════════════════════════════════════

@Composable
private fun DeveloperAvatar() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier            = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.85f))
            .padding(vertical = 28.dp, horizontal = 20.dp)
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .shadow(8.dp, CircleShape,
                    ambientColor = Color(0xFF7DD3FC).copy(alpha = 0.3f),
                    spotColor    = Color(0xFF7DD3FC).copy(alpha = 0.2f))
                .clip(CircleShape)
                .background(Color(0xFF1C1C1E)),
            contentAlignment = Alignment.Center
        ) {
            Text("👨‍💻", fontSize = 36.sp)
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "独立开发者",
            style = TextStyle(
                fontSize   = 18.sp,
                fontWeight = FontWeight.Black,
                color      = Color(0xFF1C1C1E)
            )
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "这是一个由个人独立开发的小应用\n感谢你的使用与支持 🙏",
            style     = TextStyle(fontSize = 13.sp, color = Color(0xFF94A3B8), lineHeight = 20.sp),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                "Kotlin"          to Color(0xFF818CF8),
                "Jetpack Compose" to Color(0xFF34D399),
                "Android"         to Color(0xFF60A5FA)
            ).forEach { (label, color) ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(color.copy(alpha = 0.13f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(label, style = TextStyle(fontSize = 11.sp,
                        fontWeight = FontWeight.Bold, color = color))
                }
            }
        }
    }
}

// ══════════════════════════════════════════════
//  联系原因卡片
// ══════════════════════════════════════════════

@Composable
private fun ContactReasonCard(card: ContactCard, context: Context) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(3.dp, RoundedCornerShape(18.dp),
                ambientColor = Color(0xFF94A3B8).copy(alpha = 0.10f))
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.88f))
            .clickable {
                val subject = when (card.title) {
                    "提出建议" -> "【建议】Schedule App 功能建议"
                    "给点鼓励" -> "【鼓励】来自用户的支持"
                    else      -> "【Bug】Schedule App 问题反馈"
                }
                sendEmail(context, subject)
            }
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(card.iconColor.copy(alpha = 0.13f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(card.icon, null,
                tint     = card.iconColor,
                modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(card.title,
                style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold,
                    color = Color(0xFF1C1C1E)))
            Spacer(Modifier.height(3.dp))
            Text(card.subtitle,
                style = TextStyle(fontSize = 12.sp, color = Color(0xFF94A3B8)))
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(card.iconColor.copy(alpha = 0.13f))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(card.actionLabel,
                style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    color = card.iconColor))
        }
    }
}

// ══════════════════════════════════════════════
//  分区标题
// ══════════════════════════════════════════════

@Composable
private fun SectionTitle(text: String) {
    Row(
        modifier          = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.dp).height(16.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFF1C1C1E))
        )
        Spacer(Modifier.width(8.dp))
        Text(text,
            style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold,
                color = Color(0xFF1C1C1E), letterSpacing = 1.sp))
    }
}

// ══════════════════════════════════════════════
//  底部感谢
// ══════════════════════════════════════════════

@Composable
private fun FooterNote() {
    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF1C1C1E))
            .padding(vertical = 24.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("✨", fontSize = 28.sp)
        Spacer(Modifier.height(10.dp))
        Text(
            "感谢每一位使用者",
            style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Black,
                color = Color.White)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "你的每一条反馈都会认真阅读\n让这个应用变得更好是我们共同的目标",
            style     = TextStyle(fontSize = 12.sp, color = Color(0xFF94A3B8), lineHeight = 19.sp),
            textAlign = TextAlign.Center
        )
    }
}

// ══════════════════════════════════════════════
//  Preview
// ══════════════════════════════════════════════

@Preview(showBackground = true, widthDp = 400, heightDp = 860)
@Composable
fun HelpDeveloperPreview() {
    HelpDeveloper()
}