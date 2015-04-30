package com.ikimuhendis.vine4j.types;

import com.ikimuhendis.vine4j.util.JSONUtil;
import org.json.simple.JSONObject;

public class VineTag {

	public long tagId;
	public String tag;
	
	public VineTag(JSONObject data){
		if(data!=null){
			tagId=JSONUtil.getLong(data, "tagId");
			tag=JSONUtil.getString(data, "tag");
		}
	}
	
}
