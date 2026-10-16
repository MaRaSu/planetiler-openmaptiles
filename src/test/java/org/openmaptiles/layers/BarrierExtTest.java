package org.openmaptiles.layers;

import static com.onthegomap.planetiler.TestUtils.newPoint;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.onthegomap.planetiler.FeatureCollector;
import com.onthegomap.planetiler.TestUtils;
import com.onthegomap.planetiler.reader.SimpleFeature;
import com.onthegomap.planetiler.reader.osm.OsmElement;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import org.openmaptiles.OpenMapTilesProfile;

class BarrierExtTest extends AbstractLayerTest {

  private static OsmElement.Way way(long id, Map<String, String> tags, long... nodeIds) {
    var way = new OsmElement.Way(id);
    tags.forEach(way::setTag);
    way.nodes().add(nodeIds);
    return way;
  }

  private Object wayAttrOfGate(OsmElement.Node gate, List<OsmElement.Way> ways) {
    return gateAttrs(gate, ways).get("way");
  }

  private Map<String, Object> gateAttrs(OsmElement.Node gate, List<OsmElement.Way> ways) {
    profile.preprocessOsmNode(gate);
    ways.forEach(profile::preprocessOsmWay);
    FeatureCollector features = process(SimpleFeature.create(
      newPoint(1, 2),
      gate.tags(),
      OpenMapTilesProfile.OSM_SOURCE,
      null,
      gate.id()
    ));
    var barrier = StreamSupport.stream(features.spliterator(), false)
      .filter(f -> "barrier_ext".equals(f.getLayer()))
      .findFirst()
      .orElseThrow();
    return TestUtils.toMap(barrier, 14);
  }

  private static OsmElement.Node gate(long id) {
    var node = new OsmElement.Node(id, 1, 2);
    node.setTag("barrier", "gate");
    return node;
  }

  @Test
  void testGateInBuildingPassage() {
    assertEquals("passage", wayAttrOfGate(gate(10), List.of(
      way(100, Map.of("highway", "footway", "tunnel", "building_passage"), 1, 10, 2)
    )));
  }

  @Test
  void testGateOnCoveredWay() {
    assertEquals("passage", wayAttrOfGate(gate(11), List.of(
      way(101, Map.of("highway", "service", "covered", "yes"), 1, 11)
    )));
  }

  @Test
  void testGateOnDriveway() {
    assertEquals("driveway", wayAttrOfGate(gate(12), List.of(
      way(102, Map.of("highway", "service", "service", "driveway"), 1, 12, 2)
    )));
  }

  @Test
  void testPassageWinsOverDriveway() {
    assertEquals("passage", wayAttrOfGate(gate(13), List.of(
      way(103, Map.of("highway", "service", "service", "parking_aisle"), 1, 13),
      way(104, Map.of("highway", "service", "tunnel", "building_passage"), 13, 2)
    )));
  }

  @Test
  void testGateOnOrdinaryRoad() {
    assertEquals(null, wayAttrOfGate(gate(14), List.of(
      way(105, Map.of("highway", "residential"), 1, 14, 2),
      way(106, Map.of("highway", "service", "service", "alley"), 14, 3)
    )));
  }

  @Test
  void testGateOnNamedDriveway() {
    var attrs = gateAttrs(gate(16), List.of(
      way(108, Map.of("highway", "service", "service", "driveway"), 1, 16),
      way(109, Map.of("highway", "service", "service", "parking_aisle", "name", "Pihatie"), 16, 2)
    ));
    assertEquals("driveway", attrs.get("way"));
    assertEquals(1, attrs.get("way_named"));
  }

  @Test
  void testGateOnUnnamedDriveway() {
    var attrs = gateAttrs(gate(17), List.of(
      way(110, Map.of("highway", "service", "service", "driveway", "name", ""), 1, 17, 2)
    ));
    assertEquals("driveway", attrs.get("way"));
    assertEquals(null, attrs.get("way_named"));
  }

  @Test
  void testGateInNamedPassage() {
    var attrs = gateAttrs(gate(18), List.of(
      way(111,
        Map.of("highway", "service", "service", "driveway", "tunnel", "building_passage", "name", "Porttikäytävä"),
        1, 18),
      way(112, Map.of("highway", "service", "service", "driveway", "name", "Pihatie"), 18, 2)
    ));
    assertEquals("passage", attrs.get("way"));
    assertEquals(null, attrs.get("way_named"));
  }

  @Test
  void testNonGateBarrierOnPassage() {
    var bollard = new OsmElement.Node(15, 1, 2);
    bollard.setTag("barrier", "bollard");
    assertEquals(null, wayAttrOfGate(bollard, List.of(
      way(107, Map.of("highway", "footway", "tunnel", "building_passage"), 1, 15)
    )));
  }
}
