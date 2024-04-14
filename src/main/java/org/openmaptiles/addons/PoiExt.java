package org.openmaptiles.addons;

import com.onthegomap.planetiler.FeatureCollector;
import com.onthegomap.planetiler.reader.SourceFeature;
import java.util.HashMap;
import java.util.Map;
import org.openmaptiles.Layer;
import org.openmaptiles.OpenMapTilesProfile;

public class PoiExt implements Layer, OpenMapTilesProfile.OsmAllProcessor {

  private static final String LAYER_NAME = "poi_ext";

  @Override
  public String name() {
    return LAYER_NAME;
  }

  @Override
  public void processAllOsm(SourceFeature feature, FeatureCollector features) {
    // Handle "fixme" tag specifically
    if (feature.hasTag("fixme")) {
        String initialMethod = determineInitialMethod(feature);
        String subclassValue = (String) feature.getTag("fixme");
        addFeature(features, initialMethod, "fixme", subclassValue, feature);
        return; // Skip further processing if "fixme" is handled
    }

    Map<String, String> tagToClassSubclassMap = getTagToClassSubclassMap();
    String initialMethod = determineInitialMethod(feature);

    for (Map.Entry<String, String> entry : tagToClassSubclassMap.entrySet()) {
        String[] tags = entry.getKey().split(",");
        String[] classSubclass = entry.getValue().split(",");

        boolean matchesAllTags = true;
        for (String tag : tags) {
            String[] keyValue = tag.split("=");
            if (!feature.hasTag(keyValue[0], keyValue.length > 1 ? keyValue[1] : null)) {
                matchesAllTags = false;
                break;
            }
        }

        if (matchesAllTags) {
            String clazz = classSubclass[0];
            String subclass = classSubclass.length > 1 ? classSubclass[1] : null;
            addFeature(features, initialMethod, clazz, subclass, feature);
            break; // Assuming one tag match per feature, remove if multiple matches should be processed
        }
    }
  }

  private Map<String, String> getTagToClassSubclassMap() {
      Map<String, String> tagToClassSubclass = new HashMap<>();
      // Key is tag combination; value is "class,subclass"
      tagToClassSubclass.put("man_made=tower,tower:type=observation", "tower,observation");
      tagToClassSubclass.put("amenity=shelter,shelter_type=lean_to", "shelter,lean_to");
      tagToClassSubclass.put("amenity=shelter,shelter_type=picnic_shelter", "shelter,picnic_shelter");
      tagToClassSubclass.put("amenity=shelter,shelter_type=weather_shelter", "shelter,weather_shelter");
      tagToClassSubclass.put("amenity=water_point", "water,water_point");
      tagToClassSubclass.put("amenity=drinking_water", "water,drinking_water");
      tagToClassSubclass.put("amenity=toilets", "amenity,toilets");
      tagToClassSubclass.put("building=toilets", "amenity,toilets");
      tagToClassSubclass.put("tourism=viewpoint", "tourism,viewpoint");
      tagToClassSubclass.put("tourism=wilderness_hut", "tourism,wilderness_hut");
      tagToClassSubclass.put("leisure=swimming_area", "leisure,swimming_area");
      tagToClassSubclass.put("leisure=firepit", "leisure,firepit");
      tagToClassSubclass.put("building=bunker", "building,bunker");
      tagToClassSubclass.put("military=bunker", "military,bunker");
      tagToClassSubclass.put("historic=monument", "historic,monument");
      tagToClassSubclass.put("historic=memorial", "historic,memorial");
      tagToClassSubclass.put("natural=cave_entrance", "natural,cave_entrance");
      tagToClassSubclass.put("natural=peak", "natural,peak");
      tagToClassSubclass.put("obstacle=fallen_tree", "obstacle,fallen_tree");
      tagToClassSubclass.put("noexit=yes", "noexit,yes");
      return tagToClassSubclass;
  }

  private String determineInitialMethod(SourceFeature feature) {
      if (!(feature.canBeLine() || feature.canBePolygon())) {
          return "point";
      } else if (feature.canBePolygon()) {
          return "centroidIfConvex";
      }
      return null; // or some default action
  }

  private void addFeature(FeatureCollector features, String initialMethod, String clazz, String subclass,SourceFeature feature) {
    com.onthegomap.planetiler.FeatureCollector.Feature finalFeature;
    if ("point".equals(initialMethod)) {
        finalFeature = features.point(LAYER_NAME);
    } else if ("centroidIfConvex".equals(initialMethod)) {
        finalFeature = features.centroidIfConvex(LAYER_NAME);
    } else {
        return; // or some default action
    }

    finalFeature.setBufferPixels(4)
           .setMinZoom(9)
           .setAttr("class", clazz);
    if (subclass != null && !subclass.isEmpty()) {
        finalFeature.setAttr("subclass", subclass)
          .setAttr("access", feature.getTag("access"))
          .setAttr("name", feature.getTag("name"));
        
    }
  }
}


