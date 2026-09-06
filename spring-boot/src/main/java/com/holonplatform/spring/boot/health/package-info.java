/*
 * Copyright 2016-2017 Axioma srl.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
/**
 * Optional Spring Boot Actuator {@link org.springframework.boot.health.contributor.HealthIndicator} support for
 * Holon platform components (currently {@link com.holonplatform.core.datastore.Datastore}).
 * <p>
 * Nothing in this package is auto-configured: applications must explicitly declare the desired
 * {@code HealthIndicator} bean(s), since the required check parameters (e.g. which {@code Datastore} and which
 * {@code DataTarget} to check) cannot be assumed at the library level. Once declared as a Spring bean, Spring Boot
 * Actuator (if present, via {@code spring-boot-starter-actuator}) automatically detects and exposes it through the
 * standard {@code /actuator/health} endpoint.
 * </p>
 * 
 * @since 10.0.0
 */
package com.holonplatform.spring.boot.health;
