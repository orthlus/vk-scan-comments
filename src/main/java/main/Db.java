package main;


import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class Db {
	@Autowired
	private DSLContext db;

	public List<Integer> getGroups() {
		return null;
	}
}
