package com.greenpocket.global.auth;

import java.util.Optional;

public interface DemoUserLookup {

	Optional<Long> findUserIdByDemoKey(String demoKey);
}
