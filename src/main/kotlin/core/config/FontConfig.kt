import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import java.awt.GraphicsEnvironment
import java.io.File

object FontConfig {
    
    // Myanmar font family with fallbacks
    val myanmarFontFamily: FontFamily by lazy {
        try {
            // Use Noto Sans Myanmar as primary font
            val notoFont = FontFamily(
                Font(
                    resource = "fonts/NotoSansMyanmar-Regular.ttf",
                    weight = FontWeight.Normal
                ),
                Font(
                    resource = "fonts/NotoSansMyanmar-Bold.ttf",
                    weight = FontWeight.Bold
                ),
                Font(
                    resource = "fonts/NotoSansMyanmar-Medium.ttf",
                    weight = FontWeight.Medium
                ),
                Font(
                    resource = "fonts/NotoSansMyanmar-Light.ttf",
                    weight = FontWeight.Light
                )
            )
            notoFont
        } catch (e: Exception) {
            try {
                // Fallback to Pyidaungsu font from resources
                val pyidaungsuFont = FontFamily(
                    Font(
                        resource = "fonts/pyidaungsu-1.3.ttf",
                        weight = FontWeight.Normal
                    )
                )
                pyidaungsuFont
            } catch (e2: Exception) {
                // Final fallback to system default
                FontFamily.Default
            }
        }
    }
    
    // Function to check if Myanmar text is present
    fun containsMyanmarText(text: String): Boolean {
        return text.any { char ->
            char.code in 0x1000..0x109F || // Myanmar Unicode block
            char.code in 0xAA60..0xAA7F    // Myanmar Extended-A block
        }
    }
    
    // Get appropriate font family based on text content
    fun getFontFamily(text: String): FontFamily {
        return if (containsMyanmarText(text)) {
            myanmarFontFamily
        } else {
            FontFamily.Default
        }
    }
}