package com.greenpocket.policy.external;

import java.util.List;

public record YouthPolicyPage(
	int totalCount,
	int pageNumber,
	int pageSize,
	List<YouthPolicySourcePolicy> policies
) {
}
