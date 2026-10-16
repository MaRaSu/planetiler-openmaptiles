package org.openmaptiles.addons;

import com.carrotsearch.hppc.LongArrayList;
import com.carrotsearch.hppc.LongByteMap;
import com.carrotsearch.hppc.LongHashSet;
import com.carrotsearch.hppc.LongSet;
import com.onthegomap.planetiler.FeatureCollector;
import com.onthegomap.planetiler.ForwardingProfile;
import com.onthegomap.planetiler.collection.Hppc;
import com.onthegomap.planetiler.reader.SourceFeature;
import com.onthegomap.planetiler.reader.osm.OsmElement;
import java.util.Set;
import org.openmaptiles.Layer;
import org.openmaptiles.OpenMapTilesProfile;

public class BarrierExt implements Layer, OpenMapTilesProfile.OsmAllProcessor,
  ForwardingProfile.OsmNodePreprocessor, ForwardingProfile.OsmWayPreprocessor {

  private static final String LAYER_NAME = "barrier_ext";
  private static final Set<String> ACCESS_NO_VALUES = Set.of(
    "private", "no", "military", "permit", "delivery", "customers"
  );

  // gate-like barrier nodes get a "way" attribute describing the highway way they sit on
  private static final Set<String> GATE_CLASSES = Set.of(
    "gate", "lift_gate", "swing_gate", "sliding_gate", "kissing_gate"
  );
  private static final Set<String> DRIVEWAY_SERVICE_VALUES = Set.of("driveway", "parking_aisle");
  // higher value wins when a node sits on several ways: passage over driveway
  private static final byte WAY_DRIVEWAY = 1;
  private static final byte WAY_PASSAGE = 2;

  private final LongSet gateNodes = new LongHashSet();
  private final LongByteMap gateWayContext = Hppc.newLongByteHashMap();
  // gates on at least one named driveway way; only reported when the gate's "way" is driveway
  private final LongSet namedDrivewayGates = new LongHashSet();


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
  public void preprocessOsmNode(OsmElement.Node node) {
    Object barrier = node.getTag("barrier");
    if (barrier != null && GATE_CLASSES.contains(barrier)) {
      synchronized (gateNodes) {
        gateNodes.add(node.id());
      }
    }
  }

  private static byte wayContext(OsmElement.Way way) {
    if (!way.hasTag("highway")) {
      return 0;
    }
    if (way.hasTag("tunnel", "building_passage") || way.hasTag("covered", "yes")) {
      return WAY_PASSAGE;
    }
    Object service = way.getTag("service");
    if (way.hasTag("highway", "service") && service != null && DRIVEWAY_SERVICE_VALUES.contains(service)) {
      return WAY_DRIVEWAY;
    }
    return 0;
  }

  @Override
  public void preprocessOsmWay(OsmElement.Way way) {
    byte context = wayContext(way);
    if (context == 0) {
      return;
    }
    String name = way.getString("name");
    boolean namedDriveway = context == WAY_DRIVEWAY && name != null && !name.isEmpty();
    LongArrayList nodes = way.nodes();
    for (int i = 0; i < nodes.size(); i++) {
      long node = nodes.get(i);
      if (gateNodes.contains(node)) {
        synchronized (gateWayContext) {
          if (context > gateWayContext.getOrDefault(node, (byte) 0)) {
            gateWayContext.put(node, context);
          }
          if (namedDriveway) {
            namedDrivewayGates.add(node);
          }
        }
      }
    }
  }

  private String wayAttr(SourceFeature feature) {
    byte context = gateWayContext.getOrDefault(feature.id(), (byte) 0);
    return switch (context) {
      case WAY_PASSAGE -> "passage";
      case WAY_DRIVEWAY -> "driveway";
      default -> null;
    };
  }

  private Integer wayNamedAttr(SourceFeature feature, String wayAttr) {
    return "driveway".equals(wayAttr) && namedDrivewayGates.contains(feature.id()) ? 1 : null;
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
        String way = wayAttr(feature);
        features.point(LAYER_NAME)
          .setBufferPixels(4)
          .setMinZoom(12)
          .setAttr("class", feature.getTag("barrier"))
          .setAttr("access", access(feature.getTag("access")))
          .setAttr("locked", feature.getTag("locked"))
          .setAttr("bicycle", getAccess(feature.getTag("bicycle")))
          .setAttr("foot", getAccess(feature.getTag("foot")))
          .setAttr("way", way)
          .setAttr("way_named", wayNamedAttr(feature, way));
      }
    }
  }
}
