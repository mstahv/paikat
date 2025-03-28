package at.paik;

import org.junit.jupiter.api.Test;
import org.springframework.security.web.webauthn.api.Bytes;
import tech.units.indriya.ComparableQuantity;
import tech.units.indriya.format.FormatBehavior;
import tech.units.indriya.format.NumberDelimiterQuantityFormat;
import tech.units.indriya.format.SimpleUnitFormat;
import tech.units.indriya.quantity.Quantities;

import javax.measure.format.QuantityFormat;
import javax.measure.quantity.Length;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Locale;

import static javax.measure.MetricPrefix.KILO;
import static tech.units.indriya.unit.Units.METRE;
import static tech.units.indriya.unit.Units.VOLT;

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


    @Test
    void formatMeters() {
        var quantFormat = new NumberDelimiterQuantityFormat.Builder()
                .setNumberFormat(NumberFormat.getCompactNumberInstance(Locale.of("en"), NumberFormat.Style.SHORT))
                .setUnitFormat(SimpleUnitFormat.getInstance()).build();
        var vQuant = Quantities.getQuantity(10000, VOLT);
        System.out.println(quantFormat.format(vQuant));
        var vQuant2 = Quantities.getQuantity(10, KILO(VOLT));
        System.out.println(quantFormat.format(vQuant2));
        System.out.println(vQuant.isEquivalentTo(vQuant2));


        double meters = 31412.508567098674;


        QuantityFormat qf = NumberDelimiterQuantityFormat.getInstance(FormatBehavior.LOCALE_SENSITIVE);

        ComparableQuantity<Length> quantity = Quantities.getQuantity(meters, METRE);
        System.out.println(quantFormat.format(quantity));
        System.out.println(qf.format(quantity));


/*
        var symbols = DecimalFormatSymbols.getInstance(Locale.ROOT);
        String[] cnPatterns = new String [] {"", ""};
        var compactFormat = new CompactNumberFormat("",
                        symbols, cnPatterns);
        var quantFormat2 = NumberDelimiterQuantityFormat.getCompactInstance(compactFormat, SimpleUnitFormat.getInstance());
*/


    }

}
