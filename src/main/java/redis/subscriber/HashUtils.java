package redis.subscriber;

public class HashUtils {
    public static long getHash(String str) {
        long hash = 0;
        for (int i = 0; i < str.length(); i++) {
            hash = (hash * 31) + str.charAt(i);
        }
        return hash;
    }
}
