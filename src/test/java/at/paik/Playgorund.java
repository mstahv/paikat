package at.paik;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.util.Assert;
import org.springframework.security.web.webauthn.api.Bytes;

import java.util.Arrays;

public class Playgorund {

    @Test
    void test() {
        Bytes random = Bytes.random();
        Bytes random1 = Bytes.random();
        byte[] bytes = random.getBytes();
        byte[] clone = bytes.clone();
        int compare = Arrays.compare(bytes, random1.getBytes());
        System.out.println(compare);
    }

}
