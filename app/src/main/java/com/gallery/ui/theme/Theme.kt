package com.gallery.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

private val GalleryDarkColorScheme = darkColorScheme(
    primary              = VividViolet,
    onPrimary            = DeepViolet,
    primaryContainer     = VividVioletDim,
    onPrimaryContainer   = LightVioletCont,
    secondary            = RosePink,
    onSecondary          = RosePinkDim,
    secondaryContainer   = RosePinkDim,
    onSecondaryContainer = RosePink,
    tertiary             = GoldAmber,
    background           = NearBlack,
    onBackground         = DarkOnSurface,
    surface              = DarkSurface,
    onSurface            = DarkOnSurface,
    surfaceVariant       = DarkSurfaceVar,
    onSurfaceVariant     = VividViolet,
    outline              = DarkOutline,
)

private val GalleryLightColorScheme = lightColorScheme(
    primary              = DeepIndigo,
    onPrimary            = SurfaceWhite,
    primaryContainer     = LightVioletCont,
    onPrimaryContainer   = DeepViolet,
    secondary            = DeepRose,
    onSecondary          = SurfaceWhite,
    secondaryContainer   = LightRoseCont,
    onSecondaryContainer = DeepRose,
    tertiary             = DeepAmberLight,
    background           = WarmWhite,
    onBackground         = LightOnSurface,
    surface              = SurfaceWhite,
    onSurface            = LightOnSurface,
    surfaceVariant       = LightSurfaceVar,
    onSurfaceVariant     = DeepIndigo,
    outline              = LightOutline,
)

@Composable
fun GalleryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = remember(darkTheme, dynamicColor) {
        if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            if (darkTheme) GalleryDarkColorScheme else GalleryLightColorScheme
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography  = AppTypography,
        shapes      = AppShapes,
        content     = content
    )
}
