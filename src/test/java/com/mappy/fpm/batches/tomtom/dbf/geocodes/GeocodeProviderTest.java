package com.mappy.fpm.batches.tomtom.dbf.geocodes;

import com.mappy.fpm.batches.tomtom.TomtomFolder;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.Optional;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GeocodeProviderTest {

    private final TomtomFolder tomtomFolder = mock(TomtomFolder.class);
    private GeocodeProvider geocodeProvider;

    @Before
    public void setUp() {
        when(tomtomFolder.getFile("gc.dbf")).thenReturn("src/test/resources/tomtom/geocode/andand___________gc.dbf");
        geocodeProvider = new GeocodeProvider(tomtomFolder);
    }

    @Test
    public void should_add_alternative_road_names() {
        Map<String, String> tags = geocodeProvider.getAlternateRoadNamesWithSide(10200000002470L);
        assertThat(tags).hasSize(1);
        assertThat(tags.get("name:ca")).isEqualTo("Avinguda del Consell de la Terra");
    }

    @Test
    public void should_concat_alternative_road_names() {
        Map<String, String> tags = geocodeProvider.getAlternateRoadNamesWithSide(10200000003569L);
        assertThat(tags).hasSize(1);
        assertThat(tags.get("name:ca")).isEqualTo("Carrer Maria Pla;Carrer de la Unió");
    }

    @Test
    public void should_add_alternative_road_names_on_left() {
        Map<String, String> tags = geocodeProvider.getAlternateRoadNamesWithSide(10200000000143L);
        assertThat(tags).hasSize(1);
        assertThat(tags.get("name:left:ca")).isEqualTo("Avinguda Carlemany");
    }

    @Test
    public void should_add_alternative_road_names_on_right() {
        Map<String, String> tags = geocodeProvider.getAlternateRoadNamesWithSide(10200000000176L);
        assertThat(tags).hasSize(1);
        assertThat(tags.get("name:right:ca")).isEqualTo("Carrer d'Isabelle Sandy");
    }

    @Test
    public void should_add_alternative_road_names_in_french() {
        Map<String, String> tags = geocodeProvider.getAlternateRoadNamesWithSide(10200000003341L);
        assertThat(tags).hasSize(2);
        assertThat(tags.get("name:ca")).isEqualTo("Carrer de La Unió");
        assertThat(tags.get("name:fr")).isEqualTo("Avinguda Carlemany");
    }

    @Test
    public void should_not_add_alternative_road_names_with_unknow_language() {
        Map<String, String> tags = geocodeProvider.getAlternateRoadNamesWithSide(10200000001935L);
        assertThat(tags).hasSize(0);
    }

    @Test
    public void should_add_a_postal_code() {
        Optional<String> postcodes = geocodeProvider.getLeftAndRightPostalCode(10200000000143L);
        assertThat(postcodes).isEqualTo(of("AD700"));
    }

    @Test
    public void should_add_a_postal_code_if_left_is_null() {
        Optional<String> postcodes = geocodeProvider.getLeftAndRightPostalCode(10200000000176L);
        assertThat(postcodes).isEqualTo(of(";AD700"));
    }

    @Test
    public void should_add_a_postal_code_if_right_is_null() {
        Optional<String> postcodes = geocodeProvider.getLeftAndRightPostalCode(10200000001935L);
        assertThat(postcodes).isEqualTo(of("AD500;"));
    }

    @Test
    public void should_add_postal_code_on_left_and_right() {
        Optional<String> postcodes = geocodeProvider.getLeftAndRightPostalCode(10200000003341L);
        assertThat(postcodes).isEqualTo(of("AD700;AD500"));
    }

    @Test
    public void should_add_an_interpolation_only_on_right() {
        Optional<String> postcodesLeft = geocodeProvider.getInterpolationsAddressLeft(10200000000143L);
        Optional<String> postcodesRight = geocodeProvider.getInterpolationsAddressRight(10200000000143L);
        assertThat(postcodesLeft).isEmpty() ;
        assertThat(postcodesRight).contains("odd");
    }

    @Test
    public void should_add_an_interpolation_on_each_side() {
        Optional<String> postcodesLeft = geocodeProvider.getInterpolationsAddressLeft(10200000000176L);
        Optional<String> postcodesRight = geocodeProvider.getInterpolationsAddressRight(10200000000176L);
        assertThat(postcodesLeft).contains("alphabetic");
        assertThat(postcodesRight).contains("odd");
    }

    @Test
    public void should_add_an_interpolation_only_on_left() {
        Optional<String> postcodesLeft = geocodeProvider.getInterpolationsAddressLeft(10200000001935L);
        Optional<String> postcodesRight = geocodeProvider.getInterpolationsAddressRight(10200000001935L);
        assertThat(postcodesLeft).contains("even");
        assertThat(postcodesRight).isEmpty() ;
    }

    @Test
    public void should_add_a_tomtom_irregular_interpolation() {
        Optional<String> postcodesLeft = geocodeProvider.getInterpolationsAddressLeft(10200000003341L);
        Optional<String> postcodesRight = geocodeProvider.getInterpolationsAddressRight(10200000003341L);
        assertThat(postcodesLeft).contains("irregular:tomtom");
        assertThat(postcodesRight).contains("all");
    }

    @Test
    public void should_not_have_interpolation() {
        Optional<String> postcodesLeft = geocodeProvider.getInterpolationsAddressLeft(10200000006600L);
        Optional<String> postcodesRight = geocodeProvider.getInterpolationsAddressRight(10200000006600L);
        assertThat(postcodesLeft).isEqualTo(empty());
        assertThat(postcodesRight).isEqualTo(empty());
    }

    @Test
    public void should_add_interpolation_right() {
        Map<String, String> interpolationAdress = geocodeProvider.getInterpolations(10200000000143L);
        assertThat(interpolationAdress)
                .containsOnlyKeys("interpolation:right")
                .containsEntry("interpolation:right" , "117;111") ;
    }
    @Test
    public void should_add_interpolation_left_and_right() {
        Map<String, String> interpolationAdress = geocodeProvider.getInterpolations(10200000002234L);
        assertThat(interpolationAdress)
                .containsOnlyKeys("interpolation:left", "interpolation:right")
                .containsEntry("interpolation:left" , "60;64")
                .containsEntry("interpolation:right" , "109;107") ;
    }
}