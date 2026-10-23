package org.openmaptiles.addons;

import com.onthegomap.planetiler.FeatureCollector;
import com.onthegomap.planetiler.reader.SourceFeature;
import java.util.Set;
import org.openmaptiles.Layer;
import org.openmaptiles.OpenMapTilesProfile;

/**
 * Linear leisure features that OpenMapTiles drops.
 * <p>
 * OpenMapTiles only takes {@code leisure=track} as a polygon (landuse layer, class=track). Race and sport tracks that
 * OSM maps as lines (e.g. BMX and pump tracks) never reach the tiles. This layer emits them as LineStrings:
 * <ul>
 * <li>{@code class}: "track" (OSM leisure value)</li>
 * <li>{@code sport}: raw OSM sport value, e.g. "bmx;cycling"</li>
 * <li>{@code cycling}: 1 when sport contains bmx, cycling or mtb, otherwise absent</li>
 * <li>{@code name}: OSM name</li>
 * <li>{@code oneway}: 1 when oneway or oneway:bicycle is yes, otherwise absent</li>
 * </ul>
 * Zoom 12-14, same minzoom as transportation class=raceway. Closed ways are emitted too (loop tracks); a closed way
 * tagged area=yes is a polygon only and is skipped.
 */
public class LeisureExt implements Layer, OpenMapTilesProfile.OsmAllProcessor {

  private static final String LAYER_NAME = "leisure_ext";
  private static final Set<String> CYCLING_SPORTS = Set.of("bmx", "cycling", "mtb");

  @Override
  public String name() {
    return LAYER_NAME;
  }

  static boolean isCyclingSport(Object sport) {
    if (sport == null) {
      return false;
    }
    for (String value : String.valueOf(sport).split(";")) {
      if (CYCLING_SPORTS.contains(value.trim())) {
        return true;
      }
    }
    return false;
  }

  private static boolean isOneway(SourceFeature feature) {
    return feature.hasTag("oneway", "yes", "1", "true") || feature.hasTag("oneway:bicycle", "yes", "1", "true");
  }

  @Override
  public void processAllOsm(SourceFeature feature, FeatureCollector features) {
    if (feature.canBeLine() && feature.hasTag("leisure", "track")) {
      Object sport = feature.getTag("sport");
      features.line(LAYER_NAME)
        .setBufferPixels(4)
        .setMinZoom(12)
        .setAttr("class", "track")
        .setAttr("sport", sport)
        .setAttr("cycling", isCyclingSport(sport) ? 1 : null)
        .setAttr("name", feature.getTag("name"))
        .setAttr("oneway", isOneway(feature) ? 1 : null);
    }
  }
}
