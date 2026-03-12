<template>
  <div class="container">
    <div class="page-title-row">
      <h1>{{ $t('airport.title') }}</h1>
      <FavoriteButton
        v-if="selectedAirport?.id"
        :active="isFavorite(selectedAirport.id)"
        @toggle="toggleFavorite"
      />
    </div>

    <div class="card search-card">
      <h2>{{ $t('airport.selectAirport') }}</h2>
      <div class="search-wrapper">
        <label for="airport-search" class="sr-only">{{ $t('airport.searchAriaLabel') }}</label>
        <input
          id="airport-search"
          v-model="searchQuery"
          type="text"
          :placeholder="$t('airport.searchPlaceholder')"
          class="search-input"
          role="combobox"
          aria-autocomplete="list"
          :aria-expanded="searchResults.length > 0 && showResults && !!searchQuery"
          aria-controls="airport-listbox"
          :aria-activedescendant="activeDescendant"
          @input="onSearchInput"
          @focus="showResults = true"
          @keydown="onSearchKeydown"
        />
        <ul
          v-if="searchResults.length > 0 && showResults && searchQuery"
          id="airport-listbox"
          class="search-results"
          role="listbox"
        >
          <li
            v-for="(airport, index) in searchResults"
            :id="'airport-option-' + airport.id"
            :key="airport.id"
            class="search-result-item"
            :class="{ 'search-result-active': highlightedIndex === index }"
            role="option"
            :aria-selected="highlightedIndex === index"
            @click="selectAirport(airport)"
          >
            <div class="result-code">{{ airport.airportCode }}</div>
            <div class="result-name">{{ airport.name }}</div>
            <div class="result-location">{{ airport.state ? airport.state + ', ' : '' }}{{ airport.country }}</div>
          </li>
        </ul>
      </div>
      <button
        v-if="selectedAirportCode"
        :disabled="refreshing"
        style="margin-top: 10px"
        @click="refreshWeather"
      >
        {{ refreshing ? $t('airport.refreshing') : $t('airport.refreshWeather') }}
      </button>
    </div>

    <AirportSkeleton v-if="loading" />
    <div v-if="error" class="error">{{ error }}</div>

    <div v-if="metar" class="card">
      <div class="metar-header-row">
        <h2>{{ $t('airport.metar') }}</h2>
        <button class="btn-sm btn-secondary copy-btn" @click="copyToClipboard(metar.rawText)">
          {{ copyLabel === 'metar' ? 'Copied!' : 'Copy' }}
        </button>
      </div>
      <div class="weather-report">
        <div class="report-header">
          <strong>{{ selectedAirportCode }}</strong>
          <span>{{ formatDate(metar.observationTime) }}</span>
        </div>
        <div v-if="metar.fetchedAt" style="margin-bottom: 10px">
          <FreshnessBadge :fetched-at="metar.fetchedAt" data-type="airport" />
        </div>
        <DecodedWeather :raw-text="metar.rawText" type="metar">
          <div class="report-raw">{{ metar.rawText }}</div>
          <div class="report-details">
            <div
              v-if="metar.flightCategory"
              class="flight-category"
              :class="'category-' + metar.flightCategory"
              :aria-label="'Flight category: ' + metar.flightCategory"
            >
              {{ metar.flightCategory }}
            </div>
            <div v-if="metar.temperatureCelsius != null">
              <span aria-hidden="true">🌡️</span> {{ Math.round(metar.temperatureCelsius) }}°C
            </div>
            <div v-if="metar.dewpointCelsius != null">
              <span aria-hidden="true">💧</span> Dew {{ Math.round(metar.dewpointCelsius) }}°C
            </div>
            <div v-if="metar.windSpeedKnots != null">
              <span aria-hidden="true">💨</span>
              {{ metar.windDirection != null ? metar.windDirection + '° at ' : ''
              }}{{ metar.windSpeedKnots }} kts{{
                metar.windGustKnots ? ', gusts ' + metar.windGustKnots + ' kts' : ''
              }}
            </div>
            <div v-if="metar.visibilityMiles != null">
              <span aria-hidden="true">👁️</span> Visibility: {{ metar.visibilityMiles }} mi
            </div>
            <div v-if="metar.ceilingFeet != null">
              <span aria-hidden="true">☁️</span> Ceiling {{ metar.ceilingFeet }} ft
            </div>
            <div v-if="metar.altimeterInches != null">
              <span aria-hidden="true">📊</span> Altimeter {{ metar.altimeterInches }} inHg
            </div>
            <div v-if="metar.skyCondition">
              <span aria-hidden="true">🌤️</span> Sky: {{ metar.skyCondition }}
            </div>
            <div v-if="metar.weatherConditions">
              <span aria-hidden="true">🌧️</span> {{ metar.weatherConditions }}
            </div>
            <div v-if="relativeHumidity !== null">
              <span aria-hidden="true">💦</span> RH: {{ relativeHumidity }}%
            </div>
            <div v-if="tempDewSpread !== null">
              <span aria-hidden="true" :class="{ 'spread-warn': tempDewSpread <= 3 }">🌫️</span>
              Spread: {{ tempDewSpread }}°C
              <span v-if="tempDewSpread <= 3" class="spread-alert">Fog risk</span>
            </div>
          </div>
        </DecodedWeather>
      </div>
    </div>

    <div v-if="taf" class="card">
      <div class="metar-header-row">
        <h2>{{ $t('airport.taf') }}</h2>
        <button class="btn-sm btn-secondary copy-btn" @click="copyToClipboard(taf.rawText, 'taf')">
          {{ copyLabel === 'taf' ? 'Copied!' : 'Copy' }}
        </button>
      </div>
      <div class="weather-report">
        <div class="report-header">
          <strong>{{ selectedAirportCode }}</strong>
          <span>{{ formatDate(taf.observationTime) }}</span>
        </div>
        <div v-if="taf.fetchedAt" style="margin-bottom: 10px">
          <FreshnessBadge :fetched-at="taf.fetchedAt" data-type="airport" />
        </div>
        <DecodedWeather :raw-text="taf.rawText" type="taf">
          <div class="report-raw">{{ taf.rawText }}</div>
        </DecodedWeather>
      </div>
    </div>

    <!-- Airport detail panels (only when an airport is selected and loaded) -->
    <template v-if="!loading && selectedAirportCode && (metar || taf)">

      <!-- Ground stop banner -->
      <div v-if="airportGroundStop" class="card ground-stop-banner">
        <div class="delay-header">
          <span class="delay-icon" aria-hidden="true">&#x1F6D1;</span>
          <strong>{{ airportGroundStop.airportCode }} — Ground Stop</strong>
        </div>
        <div class="delay-details">
          <span>{{ airportGroundStop.programType }}</span>
          <span v-if="airportGroundStop.reason">Reason: {{ airportGroundStop.reason }}</span>
          <span v-if="airportGroundStop.endTime">Until: {{ formatDate(airportGroundStop.endTime) }}</span>
        </div>
      </div>

      <!-- Delay status banner -->
      <div v-if="airportDelay && airportDelay.isDelayed" class="card delay-banner">
        <div class="delay-header">
          <span class="delay-icon" aria-hidden="true">&#x26A0;&#xFE0F;</span>
          <strong>{{ airportDelay.airportCode }} — {{ airportDelay.delayType }} Delay</strong>
        </div>
        <div class="delay-details">
          <span v-if="airportDelay.avgDelayMinutes">Avg: {{ airportDelay.avgDelayMinutes }} min</span>
          <span v-if="airportDelay.reason">Reason: {{ airportDelay.reason }}</span>
          <span v-if="airportDelay.trend" class="trend-badge" :class="'trend-' + airportDelay.trend">{{ airportDelay.trend }}</span>
        </div>
      </div>

      <!-- Density altitude -->
      <div v-if="densityAltitude !== null" class="card density-altitude-card">
        <div class="density-altitude-row">
          <div class="da-label">
            <span aria-hidden="true">&#x26A0;&#xFE0F;</span>
            Density Altitude
          </div>
          <div class="da-value" :class="{ 'da-high': densityAltitude > 5000, 'da-caution': densityAltitude > 2000 && densityAltitude <= 5000 }">
            {{ densityAltitude.toLocaleString() }} ft
          </div>
          <div class="da-note">
            Field elev: {{ fieldElevation?.toLocaleString() ?? 'N/A' }} ft |
            Pressure alt: {{ pressureAltitude?.toLocaleString() ?? 'N/A' }} ft
          </div>
        </div>
      </div>

      <!-- Wind compass rose -->
      <div v-if="metar?.windSpeedKnots != null && metar.windDirection != null" class="card wind-compass-card">
        <h3>Wind</h3>
        <div class="compass-layout">
          <svg class="compass-svg" viewBox="0 0 120 120">
            <!-- Compass circle -->
            <circle cx="60" cy="60" r="50" fill="none" stroke="var(--border-color, #ddd)" stroke-width="1" />
            <circle cx="60" cy="60" r="35" fill="none" stroke="var(--border-light, #eee)" stroke-width="0.5" />
            <!-- Cardinal points -->
            <text x="60" y="12" text-anchor="middle" font-size="10" fill="var(--text-secondary)">N</text>
            <text x="110" y="64" text-anchor="middle" font-size="10" fill="var(--text-secondary)">E</text>
            <text x="60" y="116" text-anchor="middle" font-size="10" fill="var(--text-secondary)">S</text>
            <text x="10" y="64" text-anchor="middle" font-size="10" fill="var(--text-secondary)">W</text>
            <!-- Tick marks -->
            <line v-for="tick in 36" :key="tick"
              :x1="60 + 46 * Math.sin((tick * 10) * Math.PI / 180)"
              :y1="60 - 46 * Math.cos((tick * 10) * Math.PI / 180)"
              :x2="60 + (tick % 9 === 0 ? 40 : 44) * Math.sin((tick * 10) * Math.PI / 180)"
              :y2="60 - (tick % 9 === 0 ? 40 : 44) * Math.cos((tick * 10) * Math.PI / 180)"
              stroke="var(--text-muted, #999)"
              :stroke-width="tick % 9 === 0 ? 1.5 : 0.5"
            />
            <!-- Wind arrow -->
            <line
              :x1="60 + 38 * Math.sin(metar.windDirection * Math.PI / 180)"
              :y1="60 - 38 * Math.cos(metar.windDirection * Math.PI / 180)"
              x2="60" y2="60"
              stroke="var(--accent, #2196F3)" stroke-width="2.5" stroke-linecap="round"
              :marker-end="'url(#wind-arrow)'"
            />
            <defs>
              <marker id="wind-arrow" markerWidth="6" markerHeight="6" refX="3" refY="3" orient="auto">
                <path d="M0,0 L6,3 L0,6 Z" fill="var(--accent, #2196F3)" />
              </marker>
            </defs>
            <!-- Center dot -->
            <circle cx="60" cy="60" r="3" fill="var(--accent, #2196F3)" />
          </svg>
          <div class="compass-info">
            <div class="compass-speed">{{ metar.windSpeedKnots }} <span class="compass-unit">kts</span></div>
            <div class="compass-dir">{{ metar.windDirection }}°</div>
            <div v-if="metar.windGustKnots" class="compass-gust">G{{ metar.windGustKnots }} kts</div>
          </div>
        </div>
      </div>

      <!-- Crosswind calculator -->
      <div v-if="metar?.windSpeedKnots && metar.windDirection != null && crosswindData.length > 0" class="card">
        <h3>Crosswind Components</h3>
        <div class="crosswind-note">Wind: {{ metar.windDirection }}° at {{ metar.windSpeedKnots }} kts{{ metar.windGustKnots ? `, gusts ${metar.windGustKnots} kts` : '' }}</div>
        <table class="crosswind-table">
          <thead>
            <tr>
              <th>Runway</th>
              <th>Headwind</th>
              <th>Crosswind</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="rwy in crosswindData" :key="rwy.runway">
              <td class="rwy-id">{{ rwy.runway }}</td>
              <td :class="{ 'tailwind': rwy.headwind < 0 }">
                {{ rwy.headwind >= 0 ? '+' : '' }}{{ rwy.headwind }} kts
                <span v-if="rwy.headwind < 0" class="wind-label">tail</span>
                <span v-else class="wind-label">head</span>
              </td>
              <td>{{ rwy.crosswind }} kts {{ rwy.crossDirection }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="airport-detail-grid">
        <!-- Left column: map + forecast + history + metar history -->
        <div class="detail-main">
          <!-- Mini map -->
          <div v-if="selectedAirport" class="card">
            <h2>Location</h2>
            <div ref="miniMapContainer" class="mini-map"></div>
            <div class="location-meta">
              {{ selectedAirport.latitude.toFixed(4) }}{{ selectedAirport.latitude >= 0 ? 'N' : 'S' }},
              {{ Math.abs(selectedAirport.longitude).toFixed(4) }}{{ selectedAirport.longitude >= 0 ? 'E' : 'W' }}
              <span v-if="selectedAirport.state || selectedAirport.country" class="location-region">
                {{ selectedAirport.state ? selectedAirport.state + ', ' : '' }}{{ selectedAirport.country }}
              </span>
            </div>
          </div>

          <!-- Weather radar thumbnail -->
          <div v-if="selectedAirport" class="card">
            <h3>Weather Radar</h3>
            <div class="radar-container">
              <img
                :src="`https://radar.weather.gov/ridge/standard/CONUS_loop.gif`"
                alt="Weather radar"
                class="radar-img"
                loading="lazy"
                @error="radarError = true"
              />
              <div v-if="radarError" class="radar-fallback">
                Radar image unavailable
              </div>
            </div>
          </div>

          <!-- Hourly forecast -->
          <div v-if="forecasts.length > 0" class="card">
            <HourlyTimeline :forecasts="forecasts" />
          </div>

          <!-- Flight category history dots -->
          <div v-if="metarHistory.length > 1" class="card">
            <h3>Flight Category Trend</h3>
            <div class="cat-dots-row">
              <div
                v-for="m in [...metarHistory].reverse()"
                :key="'dot-' + m.id"
                class="cat-dot"
                :class="'dot-' + (m.flightCategory || 'UNK')"
                :title="formatShortTime(m.observationTime) + ' — ' + (m.flightCategory || '?')"
              ></div>
            </div>
            <div class="cat-dots-labels">
              <span>Oldest</span>
              <span>Latest</span>
            </div>
          </div>

          <!-- METAR history table -->
          <div v-if="metarHistory.length > 1" class="card">
            <h3>Recent METARs</h3>
            <div class="metar-history-scroll">
              <table class="metar-history-table">
                <thead>
                  <tr>
                    <th>Time</th>
                    <th>Cat</th>
                    <th>Wind</th>
                    <th>Vis</th>
                    <th>Ceil</th>
                    <th>Temp</th>
                    <th>Dew</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="m in metarHistory" :key="m.id">
                    <td class="hist-time">{{ formatShortTime(m.observationTime) }}</td>
                    <td>
                      <span v-if="m.flightCategory" class="hist-cat" :class="'cat-' + m.flightCategory">{{ m.flightCategory }}</span>
                    </td>
                    <td>{{ m.windDirection != null ? m.windDirection + '°/' : '' }}{{ m.windSpeedKnots ?? '-' }}{{ m.windGustKnots ? 'G' + m.windGustKnots : '' }}</td>
                    <td>{{ m.visibilityMiles ?? '-' }}</td>
                    <td>{{ m.ceilingFeet != null ? m.ceilingFeet.toLocaleString() : '-' }}</td>
                    <td>{{ m.temperatureCelsius != null ? Math.round(m.temperatureCelsius) + '°' : '-' }}</td>
                    <td>{{ m.dewpointCelsius != null ? Math.round(m.dewpointCelsius) + '°' : '-' }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <!-- Ceiling/visibility trend sparkline -->
          <div v-if="metarHistory.length > 2" class="card">
            <h3>Ceiling &amp; Visibility Trend</h3>
            <div class="sparkline-container">
              <svg class="sparkline" viewBox="0 0 300 100" preserveAspectRatio="none">
                <!-- Ceiling line -->
                <polyline
                  :points="ceilingSparkline"
                  fill="none"
                  stroke="var(--accent, #2196F3)"
                  stroke-width="2"
                  vector-effect="non-scaling-stroke"
                />
                <!-- Visibility line -->
                <polyline
                  :points="visibilitySparkline"
                  fill="none"
                  stroke="var(--color-success, #43a047)"
                  stroke-width="2"
                  stroke-dasharray="4 2"
                  vector-effect="non-scaling-stroke"
                />
              </svg>
              <div class="sparkline-legend">
                <span class="legend-item"><span class="legend-line legend-ceiling"></span> Ceiling (ft)</span>
                <span class="legend-item"><span class="legend-line legend-vis"></span> Visibility (mi)</span>
              </div>
            </div>
          </div>

          <!-- Winds aloft profile -->
          <div v-if="nearbyWindsAloft.length > 0" class="card">
            <h3>Winds Aloft</h3>
            <div class="winds-aloft-scroll">
              <table class="winds-aloft-table">
                <thead>
                  <tr>
                    <th>Altitude</th>
                    <th>Wind Dir</th>
                    <th>Speed</th>
                    <th>Temp</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="w in nearbyWindsAloft" :key="w.id">
                    <td class="wa-alt">{{ w.altitudeFt.toLocaleString() }} ft</td>
                    <td>{{ w.windDirection != null ? w.windDirection + '°' : '-' }}</td>
                    <td>{{ w.windSpeedKnots != null ? w.windSpeedKnots + ' kts' : '-' }}</td>
                    <td>{{ w.temperatureCelsius != null ? w.temperatureCelsius + '°C' : '-' }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
            <div class="winds-aloft-station">Station: {{ nearbyWindsAloft[0].stationId }}</div>
          </div>

          <!-- Historical chart -->
          <HistoricalChart v-if="selectedAirport?.id" :location-id="selectedAirport.id" />
        </div>

        <!-- Right column: solar, links, nearby -->
        <div class="detail-sidebar">
          <!-- Solar data with countdown -->
          <div v-if="selectedAirport?.id" class="solar-wrapper">
            <SolarPanel :location-id="selectedAirport.id" />
            <div v-if="sunCountdown" class="sun-countdown">
              <span class="countdown-icon" aria-hidden="true">{{ sunCountdown.icon }}</span>
              {{ sunCountdown.label }} in {{ sunCountdown.time }}
            </div>
          </div>

          <!-- Nearby airports -->
          <div v-if="nearbyAirports.length > 0" class="card">
            <h3>Nearby Airports</h3>
            <div class="nearby-airports-list">
              <router-link
                v-for="na in nearbyAirports"
                :key="na.airportCode"
                :to="{ path: '/airports', query: { code: na.airportCode } }"
                class="nearby-airport-item"
              >
                <span class="na-code">{{ na.airportCode }}</span>
                <span class="na-name">{{ na.name }}</span>
                <span class="na-dist">{{ na.distNm }} nm</span>
                <span
                  v-if="na.flightCategory"
                  class="na-cat"
                  :class="'cat-' + na.flightCategory"
                >{{ na.flightCategory }}</span>
              </router-link>
            </div>
          </div>

          <!-- Charts & resources -->
          <div v-if="isUsAirport" class="card">
            <h3>Charts &amp; Resources</h3>
            <div class="resource-links">
              <a
                :href="`https://flightaware.com/resources/airport/${selectedAirportCode}/procedures`"
                target="_blank"
                rel="noopener noreferrer"
                class="resource-link"
              >
                <span class="resource-icon" aria-hidden="true">&#x1F4C4;</span>
                <div>
                  <div class="resource-title">Airport Procedures</div>
                  <div class="resource-desc">Instrument approaches &amp; diagrams</div>
                </div>
              </a>
              <a
                :href="`https://skyvector.com/airport/${faaCode}`"
                target="_blank"
                rel="noopener noreferrer"
                class="resource-link"
              >
                <span class="resource-icon" aria-hidden="true">&#x1F5FA;&#xFE0F;</span>
                <div>
                  <div class="resource-title">SkyVector Chart</div>
                  <div class="resource-desc">Sectional &amp; IFR charts</div>
                </div>
              </a>
              <a
                :href="`https://www.airnav.com/airport/${selectedAirportCode}`"
                target="_blank"
                rel="noopener noreferrer"
                class="resource-link"
              >
                <span class="resource-icon" aria-hidden="true">&#x2139;&#xFE0F;</span>
                <div>
                  <div class="resource-title">AirNav Info</div>
                  <div class="resource-desc">Runways, frequencies, FBOs</div>
                </div>
              </a>
            </div>
          </div>

          <!-- Nearby PIREPs -->
          <div v-if="nearbyPireps.length > 0" class="card">
            <h3>Nearby Pilot Reports</h3>
            <div class="nearby-list">
              <div v-for="p in nearbyPireps" :key="p.id" class="nearby-item">
                <div class="nearby-header">
                  <span class="nearby-type">{{ p.reportType }}</span>
                  <span class="nearby-time">{{ formatDate(p.observationTime) }}</span>
                </div>
                <div v-if="p.altitudeFt" class="nearby-detail">Alt: {{ p.altitudeFt.toLocaleString() }} ft</div>
                <div v-if="p.turbulenceIntensity" class="nearby-detail nearby-hazard">Turbulence: {{ p.turbulenceIntensity }}</div>
                <div v-if="p.icingIntensity" class="nearby-detail nearby-hazard">Icing: {{ p.icingIntensity }}</div>
                <div v-if="p.skyCondition" class="nearby-detail">Sky: {{ p.skyCondition }}</div>
              </div>
            </div>
          </div>

          <!-- Active SIGMETs -->
          <div v-if="nearbySigmets.length > 0" class="card">
            <h3>Active SIGMETs</h3>
            <div class="nearby-list">
              <div v-for="s in nearbySigmets" :key="s.id" class="nearby-item">
                <div class="nearby-header">
                  <span class="nearby-type">{{ s.sigmetType }}</span>
                  <span v-if="s.hazard" class="nearby-hazard">{{ s.hazard }}</span>
                </div>
                <div class="nearby-detail">Valid: {{ formatDate(s.validTimeFrom) }} - {{ formatDate(s.validTimeTo) }}</div>
                <div v-if="s.severity" class="nearby-detail">Severity: {{ s.severity }}</div>
              </div>
            </div>
          </div>

          <!-- Nearby TFRs -->
          <div v-if="nearbyTfrs.length > 0" class="card">
            <h3>Nearby TFRs</h3>
            <div class="nearby-list">
              <div v-for="t in nearbyTfrs" :key="t.id" class="nearby-item tfr-item">
                <div class="nearby-header">
                  <span class="nearby-type tfr-type">{{ t.tfrType }}</span>
                  <span class="nearby-time">{{ t.notamId }}</span>
                </div>
                <div v-if="t.description" class="nearby-detail tfr-desc">{{ t.description }}</div>
                <div class="nearby-detail">
                  {{ t.effectiveDate ? formatDate(t.effectiveDate) : '' }}
                  {{ t.expireDate ? '- ' + formatDate(t.expireDate) : '' }}
                </div>
              </div>
            </div>
          </div>

          <!-- Active CWAs -->
          <div v-if="nearbyCwas.length > 0" class="card">
            <h3>Center Weather Advisories</h3>
            <div class="nearby-list">
              <div v-for="c in nearbyCwas" :key="c.id" class="nearby-item">
                <div class="nearby-header">
                  <span class="nearby-type">{{ c.artcc }}</span>
                  <span v-if="c.hazard" class="nearby-hazard">{{ c.hazard }}</span>
                </div>
                <div class="nearby-detail">Valid: {{ formatDate(c.validTimeFrom) }} - {{ formatDate(c.validTimeTo) }}</div>
                <div v-if="c.severity" class="nearby-detail">Severity: {{ c.severity }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>

    <div v-if="!loading && !metar && selectedAirportCode" class="card">
      <p>{{ $t('airport.noData') }}</p>
      <p>{{ $t('airport.noDataHint') }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, onUnmounted, watch, nextTick, markRaw } from 'vue'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { useRoute } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useWeatherStore } from '../stores/weatherStore'
import weatherService, { type AirportWeather, type WeatherForecast, type Location } from '../services/weatherService'
import { formatDate } from '../utils/dateUtils'
import { useFavorites } from '../composables/useFavorites'
import FreshnessBadge from '../components/FreshnessBadge.vue'
import FavoriteButton from '../components/FavoriteButton.vue'
import DecodedWeather from '../components/DecodedWeather.vue'
import HourlyTimeline from '../components/HourlyTimeline.vue'
import HistoricalChart from '../components/HistoricalChart.vue'
import SolarPanel from '../components/SolarPanel.vue'
import AirportSkeleton from '../components/skeletons/AirportSkeleton.vue'

const route = useRoute()
const store = useWeatherStore()
const { airports, pireps, sigmets, delays, windsAloft, tfrs, cwas, groundStops } = storeToRefs(store)
const { isFavorite, addFavorite, removeFavorite } = useFavorites()

const selectedAirportCode = ref<string>('')
const selectedAirport = ref<Location | null>(null)
const metar = ref<AirportWeather | null>(null)
const taf = ref<AirportWeather | null>(null)
const forecasts = ref<WeatherForecast[]>([])
const metarHistory = ref<AirportWeather[]>([])
const loading = ref(false)
const refreshing = ref(false)
const error = ref<string | null>(null)
const radarError = ref(false)
const copyLabel = ref<string | null>(null)
let refreshTimeout: ReturnType<typeof setTimeout> | null = null
let copyTimeout: ReturnType<typeof setTimeout> | null = null
let countdownInterval: ReturnType<typeof setInterval> | null = null
const now = ref(Date.now())

// Mini map
const miniMapContainer = ref<HTMLElement | null>(null)
let miniMap: L.Map | null = null
let miniMapMarker: L.Marker | null = null

// Search state
const searchQuery = ref('')
const searchResults = ref<Location[]>([])
const showResults = ref(false)
const highlightedIndex = ref(-1)

const activeDescendant = computed(() => {
  if (highlightedIndex.value >= 0 && highlightedIndex.value < searchResults.value.length) {
    return 'airport-option-' + searchResults.value[highlightedIndex.value].id
  }
  return undefined
})

function onSearchInput() {
  highlightedIndex.value = -1
  if (!searchQuery.value || searchQuery.value.length < 1) {
    searchResults.value = []
    return
  }

  const query = searchQuery.value.toLowerCase()
  searchResults.value = airports.value
    .filter(
      (apt) =>
        apt.airportCode?.toLowerCase().includes(query) ||
        apt.name?.toLowerCase().includes(query),
    )
    .slice(0, 50)
  showResults.value = true
}

function onSearchKeydown(event: KeyboardEvent) {
  if (!showResults.value || searchResults.value.length === 0) return

  switch (event.key) {
    case 'ArrowDown':
      event.preventDefault()
      highlightedIndex.value = Math.min(highlightedIndex.value + 1, searchResults.value.length - 1)
      break
    case 'ArrowUp':
      event.preventDefault()
      highlightedIndex.value = Math.max(highlightedIndex.value - 1, 0)
      break
    case 'Enter':
      event.preventDefault()
      if (highlightedIndex.value >= 0) {
        selectAirport(searchResults.value[highlightedIndex.value])
      }
      break
    case 'Escape':
      event.preventDefault()
      showResults.value = false
      highlightedIndex.value = -1
      break
  }
}

function selectAirport(airport: Location) {
  selectedAirportCode.value = airport.airportCode || ''
  selectedAirport.value = airport
  searchQuery.value = `${airport.airportCode} - ${airport.name}`
  searchResults.value = []
  showResults.value = false
  radarError.value = false
  loadAirportWeather()
}

async function loadAirportWeather() {
  if (!selectedAirportCode.value) return

  loading.value = true
  error.value = null
  metar.value = null
  taf.value = null
  forecasts.value = []
  metarHistory.value = []

  try {
    const promises: Promise<unknown>[] = [
      weatherService.getLatestMetar(selectedAirportCode.value),
      weatherService.getLatestTaf(selectedAirportCode.value),
      weatherService.getAirportWeather(selectedAirportCode.value),
    ]

    if (selectedAirport.value?.id) {
      promises.push(weatherService.getForecastsByLocation(selectedAirport.value.id))
    }

    const results = await Promise.allSettled(promises)

    if (results[0].status === 'fulfilled') {
      metar.value = results[0].value as AirportWeather
    }

    if (results[1].status === 'fulfilled') {
      taf.value = results[1].value as AirportWeather
    }

    if (results[2].status === 'fulfilled') {
      const allReports = results[2].value as AirportWeather[]
      metarHistory.value = allReports
        .filter(r => r.reportType === 'METAR' || r.reportType === 'SPECI')
        .sort((a, b) => new Date(b.observationTime).getTime() - new Date(a.observationTime).getTime())
        .slice(0, 12)
    }

    if (results[3] && results[3].status === 'fulfilled') {
      forecasts.value = results[3].value as WeatherForecast[]
    }

    if (results[0].status === 'rejected' && results[1].status === 'rejected') {
      error.value = 'No weather data available for this airport'
    }
  } catch (err: unknown) {
    error.value = err instanceof Error ? err.message : 'Failed to load airport weather'
  } finally {
    loading.value = false
    nextTick(() => updateMiniMap())
  }
}

function updateMiniMap() {
  const apt = selectedAirport.value
  if (!apt?.latitude || !apt?.longitude) return

  if (!miniMapContainer.value) return

  if (!miniMap) {
    miniMap = markRaw(L.map(miniMapContainer.value, {
      zoomControl: false,
      attributionControl: false,
      dragging: false,
      scrollWheelZoom: false,
      doubleClickZoom: false,
      touchZoom: false,
    }).setView([apt.latitude, apt.longitude], 10)) as unknown as L.Map

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 18,
    }).addTo(miniMap)
  } else {
    miniMap.setView([apt.latitude, apt.longitude], 10)
  }

  if (miniMapMarker) {
    miniMapMarker.remove()
  }
  miniMapMarker = L.marker([apt.latitude, apt.longitude]).addTo(miniMap)
  miniMapMarker.bindPopup(`<strong>${apt.airportCode}</strong><br/>${apt.name}`)
}

// --- Copy raw text ---
async function copyToClipboard(text: string, label: string = 'metar') {
  try {
    await navigator.clipboard.writeText(text)
    copyLabel.value = label
    if (copyTimeout) clearTimeout(copyTimeout)
    copyTimeout = setTimeout(() => { copyLabel.value = null }, 2000)
  } catch {
    // Fallback: select text manually
  }
}

// --- Format short time for METAR history ---
function formatShortTime(iso: string): string {
  try {
    const d = new Date(iso)
    return d.toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit', timeZoneName: 'short' })
  } catch {
    return iso
  }
}

// --- Density altitude calculator ---
const fieldElevation = computed(() => {
  // Estimate from altimeter setting (standard is 29.92 inHg at sea level)
  // This is a rough estimate; real field elevation would come from airport data
  return 0 // Assume sea level default; gets refined by pressure altitude calc
})

const pressureAltitude = computed(() => {
  const m = metar.value
  if (!m?.altimeterInches) return null
  // PA = Field Elev + (29.92 - altimeter) * 1000
  const fieldElev = fieldElevation.value
  return Math.round(fieldElev + (29.92 - m.altimeterInches) * 1000)
})

const densityAltitude = computed(() => {
  const m = metar.value
  if (!m || m.temperatureCelsius == null || m.altimeterInches == null) return null
  const pa = pressureAltitude.value
  if (pa == null) return null
  // ISA temp at pressure altitude = 15 - (PA / 1000 * 2)
  const isaTemp = 15 - (pa / 1000) * 2
  const tempDev = m.temperatureCelsius - isaTemp
  // DA = PA + (120 * tempDev)
  return Math.round(pa + 120 * tempDev)
})

// --- Crosswind calculator ---
// Common US runway headings (derived from airport code patterns)
const commonRunways = computed(() => {
  // Since we don't have runway data from the API, derive common runway pairs
  // for US airports. For non-US, show nothing.
  if (!isUsAirport.value || !metar.value?.windDirection) return []
  // Generate likely runway pairs based on common configurations
  // Most airports have runways that roughly align with prevailing winds
  // We'll estimate based on the wind direction - show the closest runway alignment
  const wind = metar.value.windDirection
  // Generate runway pairs at 0, 30, 60, 90, 120, 150 degree intervals
  const runways: { id: string; heading: number }[] = []
  // Find the closest runway heading (runways are numbered in 10s of degrees)
  const baseHeading = Math.round(wind / 10) * 10
  // Show two runway pairs: one aligned and one perpendicular
  const aligned = baseHeading === 0 ? 360 : baseHeading
  const perp = aligned + 90 > 360 ? aligned + 90 - 360 : aligned + 90

  runways.push(
    { id: `${String(aligned / 10).padStart(2, '0')}/${String(((aligned + 180) % 360 || 360) / 10).padStart(2, '0')}`, heading: aligned },
    { id: `${String(perp / 10).padStart(2, '0')}/${String(((perp + 180) % 360 || 360) / 10).padStart(2, '0')}`, heading: perp },
  )
  return runways
})

interface CrosswindEntry {
  runway: string
  headwind: number
  crosswind: number
  crossDirection: string
}

const crosswindData = computed((): CrosswindEntry[] => {
  const m = metar.value
  if (!m?.windSpeedKnots || m.windDirection == null) return []
  if (commonRunways.value.length === 0) return []

  const results: CrosswindEntry[] = []
  const windDir = m.windDirection
  const windSpd = m.windSpeedKnots

  for (const rwy of commonRunways.value) {
    // For each runway pair, compute components for both ends
    const headings = [rwy.heading, (rwy.heading + 180) % 360 || 360]
    for (const hdg of headings) {
      const angle = ((windDir - hdg + 540) % 360) - 180 // -180 to 180
      const angleRad = (angle * Math.PI) / 180
      const headwind = Math.round(windSpd * Math.cos(angleRad))
      const crosswind = Math.round(Math.abs(windSpd * Math.sin(angleRad)))
      const crossDir = angle > 0 ? 'from R' : angle < 0 ? 'from L' : ''
      results.push({
        runway: String(hdg / 10).padStart(2, '0'),
        headwind,
        crosswind,
        crossDirection: crossDir,
      })
    }
  }

  // Sort: best headwind first
  results.sort((a, b) => b.headwind - a.headwind)
  return results
})

// --- Ceiling/visibility sparkline ---
function buildSparkline(data: (number | null)[], maxVal: number): string {
  const valid = data.filter((v): v is number => v != null)
  if (valid.length < 2) return ''
  const clampMax = Math.max(...valid, maxVal)
  const step = 300 / (data.length - 1)
  return data
    .map((v, i) => {
      const y = v != null ? 100 - (Math.min(v, clampMax) / clampMax) * 90 - 5 : 95
      return `${i * step},${y}`
    })
    .join(' ')
}

const ceilingSparkline = computed(() => {
  const data = [...metarHistory.value].reverse().map(m => m.ceilingFeet ?? null)
  return buildSparkline(data, 10000)
})

const visibilitySparkline = computed(() => {
  const data = [...metarHistory.value].reverse().map(m => m.visibilityMiles ?? null)
  return buildSparkline(data, 10)
})

// --- Sunrise/sunset countdown ---
const sunCountdown = computed(() => {
  // Parse solar data from the SolarPanel's locationId — we need the actual sunrise/sunset times
  // Since SolarPanel fetches its own data, we fetch it independently here
  if (!solarTimes.value) return null

  const nowMs = now.value
  const sunrise = new Date(solarTimes.value.sunrise).getTime()
  const sunset = new Date(solarTimes.value.sunset).getTime()

  let targetMs: number
  let label: string
  let icon: string

  if (nowMs < sunrise) {
    targetMs = sunrise - nowMs
    label = 'Sunrise'
    icon = '🌅'
  } else if (nowMs < sunset) {
    targetMs = sunset - nowMs
    label = 'Sunset'
    icon = '🌇'
  } else {
    // After sunset, show next sunrise (tomorrow ~= +24h from today's sunrise)
    targetMs = sunrise + 86400000 - nowMs
    label = 'Sunrise'
    icon = '🌅'
  }

  if (targetMs < 0) return null

  const hrs = Math.floor(targetMs / 3600000)
  const mins = Math.floor((targetMs % 3600000) / 60000)
  const time = hrs > 0 ? `${hrs}h ${mins}m` : `${mins}m`

  return { label, time, icon }
})

// Fetch solar data independently for countdown
interface SolarTimes { sunrise: string; sunset: string }
const solarTimes = ref<SolarTimes | null>(null)

async function fetchSolarTimes(locationId: number) {
  try {
    const data = await weatherService.getSolarData(locationId)
    solarTimes.value = { sunrise: data.sunrise, sunset: data.sunset }
  } catch {
    solarTimes.value = null
  }
}

// --- Nearby airports ---
interface NearbyAirport {
  airportCode: string
  name: string
  distNm: number
  flightCategory?: string
}

const nearbyAirports = computed((): NearbyAirport[] => {
  const apt = selectedAirport.value
  if (!apt?.latitude || !apt?.longitude) return []

  return airports.value
    .filter(a => a.airportCode && a.airportCode !== apt.airportCode && a.latitude && a.longitude)
    .map(a => {
      const distNm = Math.round(haversineNm(apt.latitude, apt.longitude, a.latitude, a.longitude))
      // Try to find flight category from delays data
      const delay = delays.value.find(d => d.airportCode === a.airportCode)
      return {
        airportCode: a.airportCode!,
        name: a.name,
        distNm,
        flightCategory: delay ? undefined : undefined, // We don't have per-airport METAR in store
      }
    })
    .filter(a => a.distNm <= 100 && a.distNm > 0)
    .sort((a, b) => a.distNm - b.distNm)
    .slice(0, 5)
})

function haversineNm(lat1: number, lon1: number, lat2: number, lon2: number): number {
  const R = 3440.065 // Earth radius in nautical miles
  const dLat = ((lat2 - lat1) * Math.PI) / 180
  const dLon = ((lon2 - lon1) * Math.PI) / 180
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos((lat1 * Math.PI) / 180) * Math.cos((lat2 * Math.PI) / 180) *
    Math.sin(dLon / 2) * Math.sin(dLon / 2)
  return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
}

// Nearby PIREPs (within ~100nm / ~1.5 degrees)
const nearbyPireps = computed(() => {
  const apt = selectedAirport.value
  if (!apt?.latitude || !apt?.longitude) return []
  const range = 1.5
  return pireps.value.filter(p =>
    Math.abs(p.latitude - apt.latitude) < range &&
    Math.abs(p.longitude - apt.longitude) < range
  ).slice(0, 10)
})

// Nearby SIGMETs
const nearbySigmets = computed(() => {
  return sigmets.value.slice(0, 5)
})

// Delay status for this airport
const airportDelay = computed(() => {
  if (!selectedAirportCode.value) return null
  return delays.value.find(d => d.airportCode === selectedAirportCode.value) || null
})

// Ground stop for this airport
const airportGroundStop = computed(() => {
  if (!selectedAirportCode.value) return null
  return groundStops.value.find(g => g.airportCode === selectedAirportCode.value) || null
})

// Relative humidity and temp/dew spread
const relativeHumidity = computed(() => {
  const m = metar.value
  if (m?.temperatureCelsius == null || m?.dewpointCelsius == null) return null
  // Magnus formula approximation
  const t = m.temperatureCelsius
  const td = m.dewpointCelsius
  const rh = 100 * Math.exp((17.625 * td) / (243.04 + td)) / Math.exp((17.625 * t) / (243.04 + t))
  return Math.round(Math.min(100, Math.max(0, rh)))
})

const tempDewSpread = computed(() => {
  const m = metar.value
  if (m?.temperatureCelsius == null || m?.dewpointCelsius == null) return null
  return Math.round((m.temperatureCelsius - m.dewpointCelsius) * 10) / 10
})

// Favorite toggle
function toggleFavorite() {
  const apt = selectedAirport.value
  if (!apt?.id) return
  if (isFavorite(apt.id)) {
    removeFavorite(apt.id)
  } else {
    addFavorite({ id: apt.id, name: apt.name, state: apt.state })
  }
}

// Nearby winds aloft (closest station)
const nearbyWindsAloft = computed(() => {
  const apt = selectedAirport.value
  if (!apt?.latitude || !apt?.longitude) return []
  if (windsAloft.value.length === 0) return []

  // Find the closest station
  const withDist = windsAloft.value
    .filter(w => w.latitude != null && w.longitude != null)
    .map(w => ({
      ...w,
      dist: Math.abs(w.latitude! - apt.latitude) + Math.abs(w.longitude! - apt.longitude),
    }))

  if (withDist.length === 0) return []

  const closestStation = withDist.sort((a, b) => a.dist - b.dist)[0].stationId

  return windsAloft.value
    .filter(w => w.stationId === closestStation)
    .sort((a, b) => a.altitudeFt - b.altitudeFt)
})

// Nearby TFRs
const nearbyTfrs = computed(() => {
  const apt = selectedAirport.value
  if (!apt?.latitude || !apt?.longitude) return []
  const range = 2.0 // ~120nm
  return tfrs.value
    .filter(t => t.latitude != null && t.longitude != null &&
      Math.abs(t.latitude! - apt.latitude) < range &&
      Math.abs(t.longitude! - apt.longitude) < range
    )
    .slice(0, 5)
})

// Nearby CWAs
const nearbyCwas = computed(() => {
  return cwas.value.slice(0, 5)
})

// FAA chart links (US airports starting with K)
const isUsAirport = computed(() => selectedAirportCode.value.startsWith('K') && selectedAirportCode.value.length === 4)
const faaCode = computed(() => isUsAirport.value ? selectedAirportCode.value.substring(1) : '')

async function refreshWeather() {
  if (!selectedAirportCode.value) return

  refreshing.value = true
  error.value = null

  try {
    await weatherService.refreshAirportWeather(selectedAirportCode.value)
    refreshTimeout = setTimeout(() => {
      loadAirportWeather()
      refreshing.value = false
    }, 2000)
  } catch (err: unknown) {
    error.value = err instanceof Error ? err.message : 'Failed to refresh airport weather'
    refreshing.value = false
  }
}

function selectByCode(code: string) {
  const airport = airports.value.find(a => a.airportCode === code)
  if (airport) {
    selectAirport(airport)
  } else {
    selectedAirportCode.value = code
    selectedAirport.value = null
    searchQuery.value = code
    loadAirportWeather()
  }
}

onMounted(async () => {
  await store.fetchAirports()
  store.fetchPireps()
  store.fetchSigmets()
  store.fetchDelays()
  store.fetchWindsAloft()
  store.fetchTfrs()
  store.fetchCwas()
  store.fetchGroundStops()

  // Update countdown every 30 seconds
  countdownInterval = setInterval(() => { now.value = Date.now() }, 30000)

  const code = route.query.code as string | undefined
  if (code) selectByCode(code)
})

watch(() => route.query.code, (newCode) => {
  if (newCode && typeof newCode === 'string') selectByCode(newCode)
})

// Fetch solar times when airport changes
watch(() => selectedAirport.value?.id, (newId) => {
  if (newId) fetchSolarTimes(newId)
  else solarTimes.value = null
})

onBeforeUnmount(() => {
  if (miniMap) {
    miniMap.remove()
    miniMap = null
    miniMapMarker = null
  }
})

onUnmounted(() => {
  if (refreshTimeout) { clearTimeout(refreshTimeout); refreshTimeout = null }
  if (copyTimeout) { clearTimeout(copyTimeout); copyTimeout = null }
  if (countdownInterval) { clearInterval(countdownInterval); countdownInterval = null }
})
</script>

<style scoped>
.search-card {
  position: relative;
}

.search-wrapper {
  position: relative;
}

.search-input {
  width: 100%;
  padding: 10px 16px;
  font-size: 16px;
  border: 2px solid var(--border-color, #ddd);
  border-radius: 8px;
  outline: none;
  transition: border-color 0.3s;
  box-sizing: border-box;
  background: var(--bg-input, #fff);
  color: var(--text-primary, #333);
}

.search-input:focus {
  border-color: var(--accent);
}

.search-results {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  margin-top: 4px;
  background: var(--bg-card, white);
  border: 1px solid var(--border-color, #ddd);
  border-radius: 8px;
  box-shadow: 0 4px 12px var(--shadow-md, rgba(0, 0, 0, 0.15));
  max-height: 300px;
  overflow-y: auto;
  z-index: 100;
  list-style: none;
  padding: 0;
  margin: 4px 0 0 0;
}

.search-result-item {
  padding: 8px 12px;
  cursor: pointer;
  border-bottom: 1px solid var(--border-light, #eee);
  transition: background-color 0.2s;
}

.search-result-item:last-child {
  border-bottom: none;
}

.search-result-item:hover,
.search-result-active {
  background-color: var(--bg-code, #f5f5f5);
}

.result-code {
  font-weight: 700;
  font-size: 14px;
  color: var(--accent);
}

.result-name {
  font-weight: 500;
  font-size: 14px;
}

.result-location {
  font-size: 12px;
  color: var(--text-secondary, #666);
  margin-top: 2px;
}

.weather-report {
  background: var(--bg-code, #f9f9f9);
  border-radius: 8px;
  padding: 12px;
}

.report-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 1rem;
}

.report-raw {
  background: var(--bg-input, #fff);
  border: 1px solid var(--border-color, #ddd);
  border-radius: 4px;
  padding: 8px;
  font-family: monospace;
  margin: 8px 0;
  white-space: pre-wrap;
  word-break: break-all;
  color: var(--text-primary, #333);
}

.report-details {
  display: flex;
  gap: 12px;
  margin-top: 8px;
  flex-wrap: wrap;
}

.flight-category {
  padding: 3px 8px;
  border-radius: 4px;
  font-weight: bold;
  color: white;
}

.category-VFR {
  background: var(--flight-vfr);
}

.category-MVFR {
  background: var(--flight-mvfr);
}

.category-IFR {
  background: var(--flight-ifr);
  color: #333;
}

.category-LIFR {
  background: var(--flight-lifr);
}

/* Copy button in METAR/TAF header */
.metar-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.copy-btn {
  font-size: 11px;
  min-width: 56px;
}

/* Delay banner */
.delay-banner {
  border-left: 4px solid var(--color-warning, #f0ad4e);
  background: var(--bg-warning, #fff8e1);
}

.delay-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  margin-bottom: 6px;
}

.delay-icon {
  font-size: 18px;
}

.delay-details {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  font-size: 13px;
  color: var(--text-secondary, #666);
}

.trend-badge {
  display: inline-block;
  padding: 1px 6px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 600;
  text-transform: capitalize;
}

.trend-increasing { background: var(--severity-high, #e53935); color: white; }
.trend-decreasing { background: var(--color-success, #43a047); color: white; }
.trend-stable { background: var(--text-muted, #999); color: white; }

/* Density altitude */
.density-altitude-card {
  padding: 10px 14px;
}

.density-altitude-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.da-label {
  font-weight: 600;
  font-size: 14px;
}

.da-value {
  font-size: 18px;
  font-weight: 700;
  font-family: monospace;
  color: var(--color-success, #43a047);
}

.da-caution {
  color: var(--color-warning, #f0ad4e);
}

.da-high {
  color: var(--severity-high, #e53935);
}

.da-note {
  font-size: 12px;
  color: var(--text-muted, #999);
  margin-left: auto;
}

/* Crosswind table */
.crosswind-note {
  font-size: 12px;
  color: var(--text-secondary, #666);
  margin-bottom: 8px;
}

.crosswind-table {
  width: 100%;
  font-size: 13px;
}

.crosswind-table th {
  text-align: left;
  font-size: 11px;
  text-transform: uppercase;
  color: var(--text-muted, #999);
  padding: 4px 8px;
  border-bottom: 1px solid var(--border-light, #eee);
}

.crosswind-table td {
  padding: 4px 8px;
}

.rwy-id {
  font-weight: 700;
  font-family: monospace;
  color: var(--accent);
}

.tailwind {
  color: var(--severity-high, #e53935);
}

.wind-label {
  font-size: 10px;
  color: var(--text-muted, #999);
  margin-left: 2px;
}

/* Two-column detail grid */
.airport-detail-grid {
  display: grid;
  grid-template-columns: 1fr 340px;
  gap: 16px;
  margin-top: 12px;
}

.detail-main,
.detail-sidebar {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* Mini map */
.mini-map {
  height: 200px;
  border-radius: 6px;
  overflow: hidden;
  margin-bottom: 8px;
}

.location-meta {
  font-size: 13px;
  color: var(--text-secondary, #666);
  font-family: monospace;
}

.location-region {
  margin-left: 8px;
  font-family: inherit;
  color: var(--text-primary, #333);
}

/* Radar */
.radar-container {
  position: relative;
  border-radius: 6px;
  overflow: hidden;
  background: var(--bg-code, #f5f5f5);
}

.radar-img {
  width: 100%;
  height: auto;
  display: block;
}

.radar-fallback {
  padding: 20px;
  text-align: center;
  color: var(--text-muted, #999);
  font-size: 13px;
}

/* METAR history table */
.metar-history-scroll {
  overflow-x: auto;
}

.metar-history-table {
  width: 100%;
  font-size: 12px;
  white-space: nowrap;
}

.metar-history-table th {
  text-align: left;
  font-size: 10px;
  text-transform: uppercase;
  color: var(--text-muted, #999);
  padding: 4px 6px;
  border-bottom: 1px solid var(--border-light, #eee);
}

.metar-history-table td {
  padding: 3px 6px;
  font-family: monospace;
  font-size: 11px;
}

.hist-time {
  color: var(--text-secondary, #666);
}

.hist-cat {
  display: inline-block;
  padding: 1px 4px;
  border-radius: 3px;
  font-weight: 700;
  font-size: 10px;
  color: white;
}

.cat-VFR { background: var(--flight-vfr); }
.cat-MVFR { background: var(--flight-mvfr); }
.cat-IFR { background: var(--flight-ifr); color: #333; }
.cat-LIFR { background: var(--flight-lifr); }

/* Sparkline */
.sparkline-container {
  padding: 4px 0;
}

.sparkline {
  width: 100%;
  height: 80px;
}

.sparkline-legend {
  display: flex;
  gap: 16px;
  justify-content: center;
  margin-top: 4px;
  font-size: 11px;
  color: var(--text-secondary, #666);
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.legend-line {
  display: inline-block;
  width: 16px;
  height: 2px;
}

.legend-ceiling {
  background: var(--accent, #2196F3);
}

.legend-vis {
  background: var(--color-success, #43a047);
  /* dashed effect via border */
  background: repeating-linear-gradient(
    90deg,
    var(--color-success, #43a047) 0 4px,
    transparent 4px 6px
  );
  height: 2px;
}

/* Solar countdown */
.solar-wrapper {
  position: relative;
}

.sun-countdown {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary, #666);
  background: var(--bg-code, #f5f5f5);
  border-radius: 0 0 8px 8px;
  margin-top: -4px;
}

.countdown-icon {
  font-size: 16px;
}

/* Nearby airports */
.nearby-airports-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.nearby-airport-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  border-radius: 6px;
  text-decoration: none;
  color: var(--text-primary, #333);
  background: var(--bg-code, #f5f5f5);
  transition: background 0.2s;
  font-size: 13px;
}

.nearby-airport-item:hover {
  background: var(--bg-hover, #eee);
}

.na-code {
  font-weight: 700;
  font-family: monospace;
  color: var(--accent);
  min-width: 48px;
}

.na-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12px;
}

.na-dist {
  font-size: 11px;
  color: var(--text-muted, #999);
  white-space: nowrap;
}

.na-cat {
  display: inline-block;
  padding: 1px 4px;
  border-radius: 3px;
  font-weight: 700;
  font-size: 10px;
  color: white;
}

/* Resource links */
.resource-links {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.resource-link {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 8px 10px;
  border-radius: 6px;
  text-decoration: none;
  color: var(--text-primary, #333);
  background: var(--bg-code, #f5f5f5);
  transition: background 0.2s;
}

.resource-link:hover {
  background: var(--bg-hover, #eee);
}

.resource-icon {
  font-size: 20px;
  flex-shrink: 0;
  line-height: 1;
  margin-top: 2px;
}

.resource-title {
  font-weight: 600;
  font-size: 13px;
}

.resource-desc {
  font-size: 12px;
  color: var(--text-secondary, #666);
  margin-top: 1px;
}

/* Nearby PIREPs / SIGMETs */
.nearby-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.nearby-item {
  padding: 8px 10px;
  border-radius: 6px;
  background: var(--bg-code, #f5f5f5);
  font-size: 13px;
}

.nearby-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.nearby-type {
  font-weight: 600;
  font-size: 12px;
  text-transform: uppercase;
  color: var(--accent);
}

.nearby-time {
  font-size: 11px;
  color: var(--text-muted, #999);
}

.nearby-detail {
  font-size: 12px;
  color: var(--text-secondary, #666);
  margin-top: 2px;
}

.nearby-hazard {
  color: var(--severity-high, #e53935);
  font-weight: 600;
}

/* Page title with favorite button */
.page-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* Ground stop banner */
.ground-stop-banner {
  border-left: 4px solid var(--severity-high, #e53935);
  background: var(--bg-warning, #fff8e1);
}

/* Humidity/spread */
.spread-alert {
  display: inline-block;
  padding: 1px 5px;
  border-radius: 3px;
  font-size: 10px;
  font-weight: 700;
  background: var(--color-warning, #f0ad4e);
  color: #333;
  margin-left: 4px;
}

/* Wind compass */
.wind-compass-card {
  padding: 10px 14px;
}

.compass-layout {
  display: flex;
  align-items: center;
  gap: 16px;
}

.compass-svg {
  width: 120px;
  height: 120px;
  flex-shrink: 0;
}

.compass-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.compass-speed {
  font-size: 28px;
  font-weight: 700;
  font-family: monospace;
  line-height: 1;
  color: var(--text-primary, #333);
}

.compass-unit {
  font-size: 14px;
  font-weight: 400;
  color: var(--text-muted, #999);
}

.compass-dir {
  font-size: 14px;
  color: var(--text-secondary, #666);
  font-family: monospace;
}

.compass-gust {
  font-size: 13px;
  font-weight: 600;
  color: var(--severity-high, #e53935);
}

/* Flight category dots */
.cat-dots-row {
  display: flex;
  gap: 3px;
  align-items: center;
  padding: 4px 0;
}

.cat-dot {
  flex: 1;
  height: 10px;
  border-radius: 2px;
  min-width: 8px;
  cursor: default;
}

.dot-VFR { background: var(--flight-vfr); }
.dot-MVFR { background: var(--flight-mvfr); }
.dot-IFR { background: var(--flight-ifr); }
.dot-LIFR { background: var(--flight-lifr); }
.dot-UNK { background: var(--text-muted, #ccc); }

.cat-dots-labels {
  display: flex;
  justify-content: space-between;
  font-size: 10px;
  color: var(--text-muted, #999);
  margin-top: 2px;
}

/* Winds aloft table */
.winds-aloft-scroll {
  overflow-x: auto;
}

.winds-aloft-table {
  width: 100%;
  font-size: 12px;
}

.winds-aloft-table th {
  text-align: left;
  font-size: 10px;
  text-transform: uppercase;
  color: var(--text-muted, #999);
  padding: 4px 6px;
  border-bottom: 1px solid var(--border-light, #eee);
}

.winds-aloft-table td {
  padding: 3px 6px;
  font-family: monospace;
  font-size: 11px;
}

.wa-alt {
  font-weight: 600;
  color: var(--accent);
}

.winds-aloft-station {
  margin-top: 6px;
  font-size: 11px;
  color: var(--text-muted, #999);
}

/* TFR items */
.tfr-item {
  border-left: 3px solid var(--severity-high, #e53935);
}

.tfr-type {
  color: var(--severity-high, #e53935) !important;
}

.tfr-desc {
  font-size: 11px;
  line-height: 1.3;
}

@media (max-width: 768px) {
  .airport-detail-grid {
    grid-template-columns: 1fr;
  }

  .density-altitude-row {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }

  .da-note {
    margin-left: 0;
  }
}
</style>
