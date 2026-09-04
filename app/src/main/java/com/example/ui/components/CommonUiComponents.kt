package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.database.SyncStatus
import com.example.ui.theme.LgesGold
import com.example.ui.theme.LgesNavy

@Composable
fun SafeInstituteLogo(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val painter = remember(context) {
        try {
            val drawable = ContextCompat.getDrawable(context, R.drawable.img_lges_logo)
            drawable?.let {
                val width = if (it.intrinsicWidth > 0) it.intrinsicWidth else 512
                val height = if (it.intrinsicHeight > 0) it.intrinsicHeight else 512
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                it.setBounds(0, 0, width, height)
                it.draw(canvas)
                BitmapPainter(bitmap.asImageBitmap())
            }
        } catch (_: Exception) {
            null
        }
    }

    if (painter != null) {
        Image(
            painter = painter,
            contentDescription = "LGES Logo",
            contentScale = ContentScale.Fit,
            modifier = modifier
        )
    } else {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(8.dp))
                .background(LgesNavy),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "LGES Logo",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun DocumentCard(
    modifier: Modifier = Modifier,
    containerColor: Color = Color.White,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.5.dp, LgesNavy),
        shadowElevation = 2.dp,
        modifier = modifier.then(
            if (onClick != null) {
                Modifier.clickable { onClick() }
            } else {
                Modifier
            }
        )
    ) {
        Box(
            modifier = Modifier
                .border(
                    width = 1.dp,
                    color = LgesGold,
                    shape = RoundedCornerShape(7.dp)
                )
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                content = content
            )
        }
    }
}

@Composable
fun SyncStatusBadge(
    status: SyncStatus,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, text) = when (status) {
        SyncStatus.SYNCED -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), "SYNCED")
        SyncStatus.PENDING -> Triple(Color(0xFFFFF3E0), Color(0xFFE65100), "PENDING")
        SyncStatus.SYNCING -> Triple(Color(0xFFE3F2FD), Color(0xFF1565C0), "SYNCING...")
        SyncStatus.FAILED -> Triple(Color(0xFFFFEBEE), Color(0xFFC62828), "SYNC FAILED")
        SyncStatus.DELETE_PENDING -> Triple(Color(0xFFFFEBEE), Color(0xFFC62828), "DELETE PENDING")
        SyncStatus.DELETE_FAILED -> Triple(Color(0xFFFFEBEE), Color(0xFFB71C1C), "DELETE RETRY")
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}
