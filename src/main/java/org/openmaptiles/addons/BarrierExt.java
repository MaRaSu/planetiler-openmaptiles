package org.openmaptiles.addons;

import com.onthegomap.planetiler.FeatureCollector;
import com.onthegomap.planetiler.reader.SourceFeature;
import java.util.Set;
import org.openmaptiles.Layer;
import org.openmaptiles.OpenMapTilesProfile;

public class BarrierExt implements Layer, OpenMapTilesProfile.OsmAllProcessor {

  private static final String LAYER_NAME = "barrier_ext";
  private static final Set<String> ACCESS_NO_VALUES = Set.of(
    "private", "no", "military", "permit", "delivery", "customers"
  );


  @Override
  public String name() {
    return LAYER_NAME;
  }

  private static String access(Object accessObj) {
    if (accessObj == null) {
      return null;
    }

    String value = String.valueOf(accessObj);
    if ("yes".equals(value) || "designated".equals(value) || "permissive".equals(value)) {
      return "yes";
      // no if "no" or "private" or "dismount" or "use_sidepath" or "use_cycleway" or "military" or "permit" or "delivery" or "customers"
    } else if (ACCESS_NO_VALUES.contains(value)) {
      return "no";
    } else if ("discouraged".equals(value)) {
      return "discouraged";
    }
    return null;
  }

  private static String getAccess(Object accessObj) {
    if (accessObj == null) {
      return null;
    }

    String value = String.valueOf(accessObj);
    if ("yes".equals(value) || "designated".equals(value) || "permissive".equals(value)) {
      return "yes";
      // no if "no" or "private" or "dismount" or "use_sidepath" or "use_cycleway" or "military" or "permit" or "delivery" or "customers"
    } else if ("no".equals(value) || "private".equals(value) || "dismount".equals(value) ||
      "use_sidepath".equals(value) ||
      "use_cycleway".equals(value)) {
      return "no";
    } else if ("discouraged".equals(value)) {
      return "discouraged";
    }
    return null;
  }

  @Override
  public void processAllOsm(SourceFeature feature, FeatureCollector features) {
    if (feature.hasTag("barrier")) {
      if (feature.canBeLine()) {
        features.line(LAYER_NAME)
          .setBufferPixels(4)
          .setMinZoom(12)
          .setAttr("class", feature.getTag("barrier"))
          .setAttr("access", access(feature.getTag("access")))
          .setAttr("bicycle", getAccess(feature.getTag("bicycle")))
          .setAttr("foot", getAccess(feature.getTag("foot")));
      } else if (!(feature.canBeLine() || feature.canBePolygon())) {
        features.point(LAYER_NAME)
          .setBufferPixels(4)
          .setMinZoom(12)
          .setAttr("class", feature.getTag("barrier"))
          .setAttr("access", access(feature.getTag("access")))
          .setAttr("locked", feature.getTag("locked"))
          .setAttr("bicycle", getAccess(feature.getTag("bicycle")))
          .setAttr("foot", getAccess(feature.getTag("foot")));
      }
    }
  }
}
