package fr.roadguard.app;
import org.junit.Test;import static org.junit.Assert.*;
public class RadarCsvInputTest {
 @Test public void headerAndMalformedRowsAreIgnoredWithoutTouchingCatalogue(){assertEquals(2,RadarCsvInput.count("lat;lon;type\n46.75;6.12;Radar fixe\nNaN;6;Radar fixe\n91;6;Radar fixe\n46.76,6.13,Radar feu rouge\n"));}
 @Test public void emptyOrNonFiniteInputIsRejected(){for(String s:new String[]{null,"","a;b;radar","Infinity;1;Radar fixe","46;181;Radar fixe","46;6;"})assertEquals(0,RadarCsvInput.count(s));}
}
