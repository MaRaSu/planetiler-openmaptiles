package org.openmaptiles.addons;

import com.onthegomap.planetiler.FeatureCollector;
import com.onthegomap.planetiler.reader.SourceFeature;
import com.onthegomap.planetiler.reader.osm.OsmElement;
import com.onthegomap.planetiler.reader.osm.OsmRelationInfo;
import java.util.List;
import java.util.Set;
import org.openmaptiles.Layer;
import org.openmaptiles.OpenMapTilesProfile;

public class LanduseExt implements Layer, OpenMapTilesProfile.OsmAllProcessor, OpenMapTilesProfile.OsmRelationPreprocessor{

  private static final String LAYER_NAME = "landuse_ext";
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
	
	@Override
	public List<OsmRelationInfo> preprocessOsmRelation(OsmElement.Relation relation) {
		if (relation.hasTag("boundary", "border_zoneo")) {
			String name = relation.getString("name");
			String access = relation.getString("access");
			return List.of(new BoundaryRelation(name, access, relation.id()));			
		}
		return null;
	}
 
  @Override
  public void processAllOsm(SourceFeature feature, FeatureCollector features) {
		var relationInfo = feature.relationInfo(BoundaryRelation.class);
		if (relationInfo.isEmpty()) {
			if (feature.canBePolygon()) {
				if (feature.hasTag("amenity", "parking")) {
					features.polygon(LAYER_NAME)
						.setBufferPixels(4)
						.setMinZoom(12)
						.setAttr("class", "parking")					
						.setAttr("access", access(feature.getTag("access")))
         		.setAttr("fee", feature.getTag("fee"))
						.setAttr("parking", feature.getTag("parking"));    
				} else if (feature.hasTag("boundary", "border_zone")) {
					features.polygon(LAYER_NAME)
						.setBufferPixels(4)
						.setMinZoom(8)
						.setAttr("class", "border_zone");	
				}
			} else if (feature.hasTag("amenity", "parking")) {
				features.point(LAYER_NAME)
					.setBufferPixels(4)
					.setMinZoom(12)
					.setAttr("class", "parking")					
					.setAttr("access", access(feature.getTag("access")))
					.setAttr("fee", feature.getTag("fee"))
					.setAttr("parking", feature.getTag("parking"));    
			}			
		} else {
			for (var boundaryInfo : relationInfo) {
				BoundaryRelation relation = boundaryInfo.relation();
				features.polygon(LAYER_NAME)
					.setBufferPixels(4)
					.setMinZoom(8)
					.setAttr("class", "border_zone")
					.setAttr("name", relation.name)
					.setAttr("access", relation.access)
					.setAttr("id", relation.id);
			}
		}
  }

	record BoundaryRelation(
	String name,
	String access,
	@Override long id
	) implements OsmRelationInfo {}
}