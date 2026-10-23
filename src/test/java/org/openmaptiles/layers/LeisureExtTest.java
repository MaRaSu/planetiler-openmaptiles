package org.openmaptiles.layers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;

class LeisureExtTest extends AbstractLayerTest {

  @Test
  void testLinearBmxTrack() {
    // tags of OSM way 1222627881 "Iso-Vilusen BMX-rata"
    var features = process(lineFeature(Map.of(
      "leisure", "track",
      "name", "Iso-Vilusen BMX-rata",
      "oneway:bicycle", "yes",
      "sport", "bmx;cycling"
    )));
    assertFeatures(14, List.of(Map.of(
      "_layer", "leisure_ext",
      "_type", "line",
      "class", "track",
      "sport", "bmx;cycling",
      "cycling", 1,
      "name", "Iso-Vilusen BMX-rata",
      "oneway", 1,
      "_minzoom", 12,
      "_maxzoom", 14
    )), features);
    assertCoversZoomRange(12, 14, "leisure_ext", features);
  }

  @Test
  void testLinearTrackOtherSport() {
    assertFeatures(14, List.of(Map.of(
      "_layer", "leisure_ext",
      "class", "track",
      "sport", "running",
      "cycling", "<null>",
      "name", "<null>",
      "oneway", "<null>"
    )), process(lineFeature(Map.of(
      "leisure", "track",
      "sport", "running"
    ))));
  }

  @Test
  void testCyclingSports() {
    for (String sport : List.of("bmx", "cycling", "mtb", "soccer; cycling")) {
      assertFeatures(14, List.of(Map.of(
        "_layer", "leisure_ext",
        "sport", sport,
        "cycling", 1
      )), process(lineFeature(Map.of(
        "leisure", "track",
        "sport", sport
      ))));
    }
  }

  @Test
  void testOneway() {
    for (var tag : List.of(Map.entry("oneway", "yes"), Map.entry("oneway", "1"), Map.entry("oneway", "true"),
      Map.entry("oneway:bicycle", "1"), Map.entry("oneway:bicycle", "true"))) {
      assertFeatures(14, List.of(Map.of(
        "_layer", "leisure_ext",
        "oneway", 1
      )), process(lineFeature(Map.of(
        "leisure", "track",
        tag.getKey(), tag.getValue()
      ))));
    }
    assertFeatures(14, List.of(Map.of(
      "_layer", "leisure_ext",
      "oneway", "<null>"
    )), process(lineFeature(Map.of(
      "leisure", "track",
      "oneway", "-1"
    ))));
  }

  @Test
  void testClosedTrackIsLineAndLandusePolygon() {
    var features = process(closedWayFeature(Map.of(
      "leisure", "track",
      "sport", "cycling"
    )));
    // (poi also emits a label point for the closed way; not relevant here)
    var layers = StreamSupport.stream(features.spliterator(), false)
      .filter(f -> f.getLayer().equals("landuse") || f.getLayer().equals("leisure_ext"))
      .map(f -> f.getLayer() + ":" + (f.isPolygon() ? "polygon" : "line"))
      .sorted()
      .toList();
    assertEquals(List.of("landuse:polygon", "leisure_ext:line"), layers);
  }

  @Test
  void testTrackAreaIsNotALine() {
    var features = process(closedWayFeature(Map.of(
      "leisure", "track",
      "area", "yes"
    )));
    StreamSupport.stream(features.spliterator(), false)
      .forEach(f -> assertEquals(false, "leisure_ext".equals(f.getLayer()), f::toString));
  }

  @Test
  void testOtherLeisureLineIgnored() {
    assertFeatures(14, List.of(), process(lineFeature(Map.of(
      "leisure", "pitch"
    ))));
  }
}
