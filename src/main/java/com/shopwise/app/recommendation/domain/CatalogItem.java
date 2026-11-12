package com.shopwise.app.recommendation.domain;

import java.math.BigDecimal;

public record CatalogItem(long id, String name, String description, BigDecimal price) {
}
