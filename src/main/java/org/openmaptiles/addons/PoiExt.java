package org.openmaptiles.addons;

import com.onthegomap.planetiler.FeatureCollector;
import com.onthegomap.planetiler.reader.SourceFeature;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.openmaptiles.Layer;
import org.openmaptiles.OpenMapTilesProfile;

public class PoiExt implements Layer, OpenMapTilesProfile.OsmAllProcessor {

  private static final String LAYER_NAME = "poi_ext";
    
  private static final Set<String> ACCESS_NO_VALUES = Set.of(
    "private", "no", "military", "permit", "delivery", "customers"
  );

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
      tagToClassSubclass.put("amenity=shelter,shelter_type=basic_hut", "shelter,lean_to");
      tagToClassSubclass.put("amenity=shelter,shelter_type=picnic_shelter", "shelter,picnic_shelter");
      tagToClassSubclass.put("amenity=shelter,shelter_type=weather_shelter", "shelter,weather_shelter");
      tagToClassSubclass.put("amenity=shelter,shelter_type=gazebo", "shelter,gazebo");
      tagToClassSubclass.put("amenity=water_point", "water,water_point");
      tagToClassSubclass.put("amenity=drinking_water", "water,drinking_water");
      tagToClassSubclass.put("amenity=toilets", "amenity,toilets");
      tagToClassSubclass.put("amenity=bar", "amenity,bar");
      tagToClassSubclass.put("amenity=pub", "amenity,pub");
      tagToClassSubclass.put("amenity=cafe", "amenity,cafe");
      tagToClassSubclass.put("amenity=fast_food", "amenity,fast_food");
      tagToClassSubclass.put("amenity=restaurant", "amenity,restaurant");
      tagToClassSubclass.put("amenity=bicycle_repair_station", "amenity,bicycle_repair_station");
      tagToClassSubclass.put("building=toilets", "amenity,toilets");
      tagToClassSubclass.put("tourism=viewpoint", "tourism,viewpoint");
      tagToClassSubclass.put("tourism=wilderness_hut", "tourism,wilderness_hut");
      tagToClassSubclass.put("tourism=attraction", "tourism,attraction");
      tagToClassSubclass.put("tourism=hotel", "tourism,hotel");
      tagToClassSubclass.put("tourism=hostel", "tourism,hostel");
      tagToClassSubclass.put("tourism=motel", "tourism,motel");
      tagToClassSubclass.put("tourism=apartment", "tourism,apartment");
      tagToClassSubclass.put("tourism=chalet", "tourism,chalet");
      tagToClassSubclass.put("tourism=camp_pitch", "tourism,camp_pitch");
      tagToClassSubclass.put("tourism=camp_site", "tourism,camp_site");
      tagToClassSubclass.put("tourism=picnic_site", "tourism,picnic_site");
      tagToClassSubclass.put("tourism=artwork", "tourism,artwork");
      tagToClassSubclass.put("leisure=swimming_area", "leisure,swimming_area");
      tagToClassSubclass.put("leisure=firepit", "leisure,firepit");
      tagToClassSubclass.put("leisure=picnic_table", "leisure,picnic_table");
      tagToClassSubclass.put("leisure=park", "leisure,park");
      tagToClassSubclass.put("building=bunker", "building,bunker");
      tagToClassSubclass.put("military=bunker", "military,bunker");
      tagToClassSubclass.put("historic=monument", "historic,monument");
      tagToClassSubclass.put("historic=memorial", "historic,memorial");
      tagToClassSubclass.put("natural=cave_entrance", "natural,cave_entrance");
      tagToClassSubclass.put("natural=peak", "natural,peak");
      tagToClassSubclass.put("obstacle=fallen_tree", "obstacle,fallen_tree");
      tagToClassSubclass.put("noexit=yes", "noexit,yes");
      tagToClassSubclass.put("shop=bakery", "shop,bakery");
      tagToClassSubclass.put("shop=coffee", "shop,coffee");
      tagToClassSubclass.put("shop=convenience", "shop,convenience");
      tagToClassSubclass.put("shop=department_store", "shop,department_store");
      tagToClassSubclass.put("shop=food", "shop,food");
      tagToClassSubclass.put("shop=general", "shop,general");
      tagToClassSubclass.put("shop=ice_cream", "shop,ice_cream");
      tagToClassSubclass.put("shop=mall", "shop,mall");
      tagToClassSubclass.put("shop=pastry", "shop,pastry");
      tagToClassSubclass.put("shop=supermarket", "shop,supermarket");
       tagToClassSubclass.put("ford=yes", "ford,yes");

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

    Object access = feature.getTag("access");
    if (access != null && ACCESS_NO_VALUES.contains(access)) {
        return;
    }

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
          .setAttr("name", feature.getTag("name"))
          .setAttr("website", feature.getTag("website"))
          .setAttr("url", feature.getTag("url"));
        
    }
  }
}


