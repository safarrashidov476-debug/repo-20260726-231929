package com.github.olga_yakovleva.rhvoice.android;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * RHVoice sintez qilishdan oldin matndagi raqamlarni o'qishga qulay
 * guruhlarga ajratadi. Har doim yoniq - o'chirib bo'lmaydi:
 *
 *   +998949835707      ->  +998 94 983 57 07        (telefon raqami)
 *   949835707          ->  94 983 57 07             (telefon raqami)
 *   9860082546068113   ->  98 60 08 25 46 06 81 13  (16 xonali - umumiy qoida)
 *   9403               ->  94 03                    (4 xonali - umumiy qoida)
 *   58604               (5 xonali) ->  58 60 4       (oxirgi toq raqam yakka)
 *
 * Umumiy qoida: 4 va undan uzunroq har qanday raqam ketma-ketligi
 * boshidan boshlab ikkitadan guruhlanadi; agar uzunlik toq bo'lsa,
 * oxirgi bitta raqam yakka holda qoladi.
 */
final class PhoneNumberPreprocessor {

    // +998 bilan boshlangan telefon raqami
    private static final Pattern INTL_PATTERN =
        Pattern.compile("\\+998(\\d{2})(\\d{3})(\\d{2})(\\d{2})\\b");

    // Mahalliy 9 xonali telefon raqami
    private static final Pattern LOCAL_9_PATTERN =
        Pattern.compile("(?<!\\d)(\\d{2})(\\d{3})(\\d{2})(\\d{2})(?!\\d)");

    // Formatlanmagan, kamida 4 xonali har qanday raqam ketma-ketligi
    private static final Pattern GENERIC_DIGITS_PATTERN =
        Pattern.compile("\\d{4,}");

    private PhoneNumberPreprocessor() {
    }

    static String process(String text) {
        if (text == null || text.isEmpty())
            return text;

        String result = applyGroups(text, INTL_PATTERN, "+998 ", 4);
        result = applyLocal9(result);
        result = applyGenericPairing(result);
        return result;
    }

    private static String applyGroups(String text, Pattern pattern, String prefix, int groupCount) {
        Matcher matcher = pattern.matcher(text);
        StringBuilder sb = new StringBuilder();
        int lastEnd = 0;
        while (matcher.find()) {
            sb.append(text, lastEnd, matcher.start());
            sb.append(prefix);
            for (int i = 1; i <= groupCount; i++) {
                sb.append(matcher.group(i));
                if (i < groupCount)
                    sb.append(' ');
            }
            lastEnd = matcher.end();
        }
        sb.append(text.substring(lastEnd));
        return sb.toString();
    }

    private static String applyLocal9(String text) {
        Matcher matcher = LOCAL_9_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder();
        int lastEnd = 0;
        while (matcher.find()) {
            int start = matcher.start();
            if (start >= 5 && text.startsWith("+998 ", start - 5)) {
                sb.append(text, lastEnd, matcher.end());
                lastEnd = matcher.end();
                continue;
            }
            sb.append(text, lastEnd, start);
            sb.append(matcher.group(1)).append(' ')
              .append(matcher.group(2)).append(' ')
              .append(matcher.group(3)).append(' ')
              .append(matcher.group(4));
            lastEnd = matcher.end();
        }
        sb.append(text.substring(lastEnd));
        return sb.toString();
    }

    // 4 va undan uzunroq HAR QANDAY (hali formatlanmagan) raqam ketma-ketligini
    // ikkitadan guruhlaydi; toq uzunlikda oxirgi raqam yakka qoladi.
    private static String applyGenericPairing(String text) {
        Matcher matcher = GENERIC_DIGITS_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder();
        int lastEnd = 0;
        while (matcher.find()) {
            sb.append(text, lastEnd, matcher.start());
            sb.append(pairify(matcher.group()));
            lastEnd = matcher.end();
        }
        sb.append(text.substring(lastEnd));
        return sb.toString();
    }

    private static String pairify(String digits) {
        int len = digits.length();
        int pairedLen = (len % 2 == 0) ? len : len - 1;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pairedLen; i += 2) {
            if (i > 0)
                sb.append(' ');
            sb.append(digits.charAt(i)).append(digits.charAt(i + 1));
        }
        if (len % 2 != 0) {
            if (sb.length() > 0)
                sb.append(' ');
            sb.append(digits.charAt(len - 1));
        }
        return sb.toString();
    }
}
