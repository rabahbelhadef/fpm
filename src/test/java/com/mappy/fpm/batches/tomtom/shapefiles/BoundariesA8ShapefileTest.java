package com.mappy.fpm.batches.tomtom.shapefiles;

import com.mappy.fpm.batches.AbstractTest;
import com.mappy.fpm.batches.tomtom.Tomtom2OsmTestUtils.PbfContent;
import com.mappy.fpm.batches.tomtom.TomtomFolder;
import com.mappy.fpm.batches.tomtom.dbf.names.NameProvider;
import com.mappy.fpm.batches.tomtom.helpers.OsmLevelGenerator;
import com.mappy.fpm.batches.tomtom.helpers.TownTagger;
import com.mappy.fpm.batches.tomtom.helpers.TownTagger.Centroid;
import com.mappy.fpm.batches.utils.GeometrySerializer;
import com.mappy.fpm.batches.utils.OsmosisSerializer;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.impl.PackedCoordinateSequence;
import net.morbz.osmonaut.osm.Entity;
import net.morbz.osmonaut.osm.Tags;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static com.google.common.collect.ImmutableMap.of;
import static com.mappy.fpm.batches.tomtom.Tomtom2OsmTestUtils.read;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BoundariesA8ShapefileTest extends AbstractTest {

    private static PbfContent pbfContent;

    private static NameProvider nameProvider = mock(NameProvider.class);

    @BeforeClass
    public static void setup() throws Exception {

        when(nameProvider.getAlternateNames(10560000000250L)) //
                .thenReturn(of("name", "Anderlecht", "name:nl", "AnderlechtNL", "name:fr", "AnderlechtFR"));
        when(nameProvider.getAlternateNames(10560000000267L)) //
                .thenReturn(of("name", "Sint-Gillis", "name:nl", "Sint-GillisNL", "name:fr", "Sint-GillisFR"));
        when(nameProvider.getAlternateNames(10560000000263L)) //
                .thenReturn(of("name", "Vorst", "name:nl", "VorstNL", "name:fr", "VorstFR"));

        when(nameProvider.getAlternateCityNames(10560000718742L)) //
                .thenReturn(of("name", "Anderlecht", "name:nl", "AnderlechtCNL", "name:fr", "AnderlechtCFR"));
        when(nameProvider.getAlternateCityNames(10560000388234L)) //
                .thenReturn(of("name", "Sint-Gillis", "name:nl", "Sint-GillisCNL", "name:fr", "Sint-GillisCFR"));
        when(nameProvider.getAlternateCityNames(10560000455427L)) //
                .thenReturn(of("name", "Vorst", "name:nl", "VorstCNL", "name:fr", "VorstCFR"));

        TomtomFolder tomtomFolder = mock(TomtomFolder.class);
        when(tomtomFolder.getFile("a8.shp")).thenReturn("src/test/resources/tomtom/boundaries/a8/Anderlecht___________a8.shp");

        OsmLevelGenerator osmLevelGenerator = mock(OsmLevelGenerator.class);
        when(osmLevelGenerator.getOsmLevel("Anderlecht", "8")).thenReturn("8");

        TownTagger townTagger = mock(TownTagger.class);
        double[] doubles = {4.3451859, 50.8251293};
        GeometryFactory factory = mock(GeometryFactory.class);
        Point point = new Point(new PackedCoordinateSequence.Double(doubles, 2), factory);
        when(townTagger.get(10560000718742L)).thenReturn(new Centroid(10560000718742L, "Anderlecht", 8, 1, 7, point));
        double[] doubles2 = {4.3134424, 50.8055758};
        Point point2 = new Point(new PackedCoordinateSequence.Double(doubles2, 2), factory);
        when(townTagger.get(10560000388234L)).thenReturn(new Centroid(10560000388234L, "Sint-Gillis", 8, 1, 8, point2));
        double[] doubles3 = {4.307077, 50.8366041};
        Point point3 = new Point(new PackedCoordinateSequence.Double(doubles3, 2), factory);
        when(townTagger.get(10560000455427L)).thenReturn(new Centroid(10560000455427L, "Vorst", 8, 1, 8, point3));

        BoundariesA8Shapefile shapefile = new BoundariesA8Shapefile(tomtomFolder, nameProvider, osmLevelGenerator, townTagger);

        GeometrySerializer serializer = new OsmosisSerializer("target/tests/Anderlecht.osm.pbf", "Test_TU");

        shapefile.serialize(serializer);
        serializer.close();

        pbfContent = read(new File("target/tests/Anderlecht.osm.pbf"));
        assertThat(pbfContent.getRelations()).hasSize(3);
    }

    @Test
    public void should_have_relations_with_all_tags() throws Exception {

        List<Tags> tags = pbfContent.getRelations().stream()
                .map(Entity::getTags)
                .collect(toList());

        assertThat(tags).extracting(t -> t.get("boundary")).containsOnly("administrative");
        assertThat(tags).extracting(t -> t.get("admin_level")).containsOnly("8");
        assertThat(tags).extracting(t -> t.get("type")).containsOnly("boundary");
        assertThat(tags).extracting(t -> t.get("name")).containsOnly("Anderlecht", "Sint-Gillis", "Vorst");
        assertThat(tags).extracting(t -> t.get("name:fr")).containsOnly("AnderlechtFR", "Sint-GillisFR", "VorstFR");
        assertThat(tags).extracting(t -> t.get("name:nl")).containsOnly("AnderlechtNL", "Sint-GillisNL", "VorstNL");
        assertThat(tags).extracting(t -> t.get("population")).containsOnly("116332", "50472", "55012");
        assertThat(tags).extracting(t -> t.get("ref:INSEE")).containsOnly("21001", "21013", "21007");
        assertThat(tags).extracting(t -> t.get("ref:tomtom")).containsOnly("10560000000250", "10560000000267", "10560000000263");
    }

    @Test
    public void should_have_relations_with_tags_and_role_outer() throws Exception {

        List<Tags> tags = pbfContent.getRelations().stream()
                .flatMap(f -> f.getMembers().stream())
                .filter(relationMember -> "outer".equals(relationMember.getRole()))
                .map(m -> m.getEntity().getTags())
                .collect(toList());

        assertThat(tags).hasSize(17);
        assertThat(tags).extracting(t -> t.get("name")).containsOnly("Anderlecht", "Sint-Gillis", "Vorst");
        assertThat(tags).extracting(t -> t.get("boundary")).containsOnly("administrative");
        assertThat(tags).extracting(t -> t.get("admin_level")).containsOnly("8");
    }

    @Test
    public void should_have_relation_with_role_label_and_tag_name() throws Exception {

        List<Tags> tags = pbfContent.getRelations().stream()
                .flatMap(f -> f.getMembers().stream())
                .filter(relationMember -> "label".equals(relationMember.getRole()))
                .map(m -> m.getEntity().getTags())
                .collect(toList());

        assertThat(tags).hasSize(3);
        assertThat(tags).extracting(t -> t.get("ref:tomtom")).containsOnly("10560000000250", "10560000000267", "10560000000263");
        assertThat(tags).extracting(t -> t.get("name")).containsOnly("Anderlecht", "Sint-Gillis", "Vorst");
        assertThat(tags).extracting(t -> t.get("name:fr")).containsOnly("AnderlechtFR", "Sint-GillisFR", "VorstFR");
        assertThat(tags).extracting(t -> t.get("ref:INSEE")).containsOnly("21001", "21013", "21007");
        assertThat(tags).extracting(t -> t.get("name:nl")).containsOnly("AnderlechtNL", "Sint-GillisNL", "VorstNL");
        assertThat(tags).extracting(t -> t.get("population")).containsOnly("116332", "50472", "55012");
    }

    @Test
    public void should_have_Anderlecht_as_capital() throws Exception {
        List<Tags> tags = pbfContent.getNodes().stream()
                .filter(node -> node.getTags().size() != 0)
                .map(Entity::getTags)
                .collect(toList());

        assertThat(tags).hasSize(6);
        assertThat(tags).extracting(t -> t.get("name")).containsOnly("Anderlecht", "Sint-Gillis", "Vorst");
        assertThat(tags).extracting(t -> t.get("name:fr")).containsOnly("AnderlechtCFR", "AnderlechtFR", "Sint-GillisCFR", "Sint-GillisFR", "VorstCFR", "VorstFR");
        assertThat(tags).extracting(t -> t.get("name:nl")).containsOnly("AnderlechtCNL", "AnderlechtNL", "Sint-GillisCNL", "Sint-GillisNL", "VorstCNL", "VorstNL");
        assertThat(tags).extracting(t -> t.get("population")).containsOnly("116332", "50472", "55012");
    }
}