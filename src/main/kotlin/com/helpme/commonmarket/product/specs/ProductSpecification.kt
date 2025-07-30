package com.helpme.commonmarket.product.specs

import com.helpme.commonmarket.product.entity.Product
import org.springframework.data.jpa.domain.Specification
import jakarta.persistence.criteria.Predicate

object ProductSpecification {

    fun byFilter(filter: Map<String, String>): Specification<Product> {
        return Specification { root, _, builder ->
            val predicates = mutableListOf<Predicate>()

            filter.forEach { (key, value) ->
                when (key) {
                    "name", "content" -> predicates.add(builder.like(root.get(key), "%$value%"))
                    "price", "sellerId" -> predicates.add(builder.equal(root.get<Long>(key), value.toLongOrNull()))
                }
            }
            builder.and(*predicates.toTypedArray())
        }
    }
}
