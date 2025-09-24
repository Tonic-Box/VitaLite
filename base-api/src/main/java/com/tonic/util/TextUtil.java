package com.tonic.util;

public class TextUtil
{
    static final char[] cp1252AsciiExtension = new char[]{'\u20AC', '\u0000', '\u201A', '\u0192', '\u201E', '\u2026', '\u2020', '\u2021', '\u02C6', '\u2030', '\u0160', '\u2039', '\u0152', '\u0000', '\u017D', '\u0000', '\u0000', '\u2018', '\u2019', '\u201C', '\u201D', '\u2022', '\u2013', '\u2014', '\u02DC', '\u2122', '\u0161', '\u203A', '\u0153', '\u0000', '\u017E', '\u0178'};

    public static String decodeStringCp1252(byte[] var0, int var1, int var2) {
        char[] var3 = new char[var2];
        int var4 = 0;

        for(int var5 = 0; var5 < var2; ++var5) {
            int var6 = var0[var5 + var1] & 255;
            if (var6 != 0) {
                if (var6 >= 128 && var6 < 160) {
                    char var7 = cp1252AsciiExtension[var6 - 128];
                    if (var7 == 0) {
                        var7 = '?';
                    }

                    var6 = var7;
                }

                var3[var4++] = (char)var6;
            }
        }

        return new String(var3, 0, var4);
    }

    public static int encodeStringCp1252(CharSequence var0, int var1, int var2, byte[] var3, int var4) {
        int var5 = var2 - var1;

        for(int var6 = 0; var6 < var5; ++var6) {
            char var7 = var0.charAt(var6 + var1);
            if((var7 <= 0 || var7 >= 128) && (var7 < 160 || var7 > 255)) {
                if(var7 == 8364) {
                    var3[var6 + var4] = -128;
                } else if(var7 == 8218) {
                    var3[var6 + var4] = -126;
                } else if(var7 == 402) {
                    var3[var6 + var4] = -125;
                } else if(var7 == 8222) {
                    var3[var6 + var4] = -124;
                } else if(var7 == 8230) {
                    var3[var6 + var4] = -123;
                } else if(var7 == 8224) {
                    var3[var6 + var4] = -122;
                } else if(var7 == 8225) {
                    var3[var6 + var4] = -121;
                } else if(var7 == 710) {
                    var3[var6 + var4] = -120;
                } else if(var7 == 8240) {
                    var3[var6 + var4] = -119;
                } else if(var7 == 352) {
                    var3[var6 + var4] = -118;
                } else if(var7 == 8249) {
                    var3[var6 + var4] = -117;
                } else if(var7 == 338) {
                    var3[var6 + var4] = -116;
                } else if(var7 == 381) {
                    var3[var6 + var4] = -114;
                } else if(var7 == 8216) {
                    var3[var6 + var4] = -111;
                } else if(var7 == 8217) {
                    var3[var6 + var4] = -110;
                } else if(var7 == 8220) {
                    var3[var6 + var4] = -109;
                } else if(var7 == 8221) {
                    var3[var6 + var4] = -108;
                } else if(var7 == 8226) {
                    var3[var6 + var4] = -107;
                } else if(var7 == 8211) {
                    var3[var6 + var4] = -106;
                } else if(var7 == 8212) {
                    var3[var6 + var4] = -105;
                } else if(var7 == 732) {
                    var3[var6 + var4] = -104;
                } else if(var7 == 8482) {
                    var3[var6 + var4] = -103;
                } else if(var7 == 353) {
                    var3[var6 + var4] = -102;
                } else if(var7 == 8250) {
                    var3[var6 + var4] = -101;
                } else if(var7 == 339) {
                    var3[var6 + var4] = -100;
                } else if(var7 == 382) {
                    var3[var6 + var4] = -98;
                } else if(var7 == 376) {
                    var3[var6 + var4] = -97;
                } else {
                    var3[var6 + var4] = 63;
                }
            } else {
                var3[var6 + var4] = (byte)var7;
            }
        }

        return var5;
    }

    public static String decodeUtf8(byte[] bytes, int offset, int length) {
        char[] chars = new char[length];
        int charIndex = 0;
        int byteIndex = offset;

        while (byteIndex < offset + length) {
            int b1 = bytes[byteIndex++] & 0xFF;
            int codePoint;
            if (b1 < 128) {
                codePoint = b1;
            } else if (b1 < 192) {
                codePoint = 65533;
            } else if (b1 < 224) {
                if (byteIndex < offset + length && (bytes[byteIndex] & 192) == 128) {
                    int b2 = bytes[byteIndex++] & 0x3F;
                    codePoint = ((b1 & 31) << 6) | b2;
                    if (codePoint < 128) {
                        codePoint = 65533;
                    }
                } else {
                    codePoint = 65533;
                }
            } else if (b1 < 240) {
                if (byteIndex + 1 < offset + length && (bytes[byteIndex] & 192) == 128 && (bytes[byteIndex + 1] & 192) == 128) {
                    int b2 = bytes[byteIndex++] & 0x3F;
                    int b3 = bytes[byteIndex++] & 0x3F;
                    codePoint = ((b1 & 15) << 12) | (b2 << 6) | b3;
                    if (codePoint < 2048) {
                        codePoint = 65533;
                    }
                } else {
                    codePoint = 65533;
                }
            } else if (b1 < 248) {
                if (byteIndex + 2 < offset + length && (bytes[byteIndex] & 192) == 128 && (bytes[byteIndex + 1] & 192) == 128 && (bytes[byteIndex + 2] & 192) == 128) {
                    int b2 = bytes[byteIndex++] & 0x3F;
                    int b3 = bytes[byteIndex++] & 0x3F;
                    int b4 = bytes[byteIndex++] & 0x3F;
                    codePoint = ((b1 & 7) << 18) | (b2 << 12) | (b3 << 6) | b4;
                    if (codePoint >= 65536 && codePoint <= 1114111) {
                        codePoint = 65533;
                    } else {
                        codePoint = 65533;
                    }
                } else {
                    codePoint = 65533;
                }
            } else {
                codePoint = 65533;
            }

            chars[charIndex++] = (char) codePoint;
        }

        return new String(chars, 0, charIndex);
    }

    public static int encodeUtf8(byte[] dest, int destOffset, CharSequence str) {
        int length = str.length();
        int destPos = destOffset;

        for (int i = 0; i < length; i++) {
            char c = str.charAt(i);
            if (c <= 0x7F) {
                dest[destPos++] = (byte) c;
            } else if (c <= 0x7FF) {
                dest[destPos++] = (byte) (0xC0 | c >> 6);
                dest[destPos++] = (byte) (0x80 | c & 0x3F);
            } else {
                dest[destPos++] = (byte) (0xE0 | c >> 12);
                dest[destPos++] = (byte) (0x80 | c >> 6 & 0x3F);
                dest[destPos++] = (byte) (0x80 | c & 0x3F);
            }
        }

        return destPos - destOffset;
    }

    public static boolean containsIgnoreCase(String target, String... names) {
        for (String name : names) {
            if (target.toLowerCase().contains(name.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    public static String sanitize(String text)
    {
        if(text == null)
        {
            return null;
        }
        String cleaned = text.replace('\u00A0', ' ').replace('_', ' ');
        return (cleaned.contains("<img") ? cleaned.substring(text.lastIndexOf('>') + 1) : cleaned).trim().replaceAll("<[^>]+>", "");
    }

    public static String indent(String text) {
        return text.replaceAll("(?m)^", "\t");
    }

    public static String capitalizeFirstLetter(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }
}
