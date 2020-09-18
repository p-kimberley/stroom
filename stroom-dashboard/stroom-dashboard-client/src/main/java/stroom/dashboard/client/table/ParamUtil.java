package stroom.dashboard.client.table;

public class ParamUtil {
//    public static String getParam(final String string) {
//        if (string == null) {
//            return null;
//        }
//
//        int end = 0;
//        int start = string.indexOf("${", end);
//        if (start != -1) {
//            end = string.indexOf("}", start);
//            if (end != -1) {
//                return string.substring(start + 2, end);
//            }
//        }
//
//        return null;
//    }
//
//    public static List<String> getParams(final String string) {
//        if (string == null) {
//            return null;
//        }
//
//        final List<String> params = new ArrayList<>();
//        int end = 0;
//        int start = 0;
//
//        while (start != -1) {
//            start = string.indexOf("${", end);
//            if (start != -1) {
//                end = string.indexOf("}", start);
//                if (end != -1) {
//                    final String param = string.substring(start + 2, end);
//                    params.add(param);
//                }
//            }
//        }
//
//        return params;
//    }

    public static String makeParam(final String param) {
        return "${" + param + "}";
    }
}
