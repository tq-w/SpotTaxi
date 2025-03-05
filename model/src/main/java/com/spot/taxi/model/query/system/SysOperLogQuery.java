package com.spot.taxi.model.query.system;

import lombok.Data;

@Data
public class SysOperLogQuery {

	private String title;
	private String operName;

	private String createTimeBegin;
	private String createTimeEnd;

}

