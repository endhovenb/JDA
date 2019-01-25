/*
 * Copyright 2015-2019 Austin Keener, Michael Ritter, Florian Spieß, and the JDA contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.dv8tion.jda.api;

import net.dv8tion.jda.internal.utils.Checks;

import java.awt.Color;

public enum DefaultColors
{
    TEAL_BLUE(  0x1ABC9C),
    OCEAN_BLUE( 0x11806A),
    NEON_GREEN( 0x2ECC71),
    GREEN(      0x1F8B4C),
    BLUE(       0x3498DB),
    DARK_BLUE(  0x71368A),
    PURPLE(     0x9B59B6),
    DARK_PURPLE(0x71368A),
    PINK(       0xE91E63),
    MAGENTA(    0xAD1457),
    YELLOW(     0xF1C40F),
    ORANGE(     0xC27C0E),
    ;

    private final int rgb;

    DefaultColors(int rgb)
    {
        this.rgb = rgb;
    }

    public int getRGB()
    {
        return rgb;
    }

    public Color getColor()
    {
        return new Color(rgb);
    }

    public static boolean isDefaultColor(int rgb)
    {
        rgb &= 0xFFFFFF;
        for (DefaultColors c : values())
        {
            if (c.rgb == rgb)
                return true;
        }
        return false;
    }

    public static boolean isDefaultColor(Color color)
    {
        Checks.notNull(color, "Color");
        return isDefaultColor(color.getRGB());
    }
}
