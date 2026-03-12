import { describe, it, expect } from 'vitest'
import { parseMetar, parseTaf } from '../../utils/metarTafParser'

describe('parseMetar', () => {
  it('parses a standard METAR', () => {
    const raw = 'METAR KJFK 121856Z 31015G25KT 10SM FEW250 M02/M17 A3042 RMK AO2 SLP308'
    const result = parseMetar(raw)

    expect(result.station).toBe('KJFK')
    expect(result.wind).toContain('310')
    expect(result.wind).toContain('15 knots')
    expect(result.wind).toContain('gusting to 25')
    expect(result.visibility).toBe('10 miles')
    expect(result.skyLayers).toHaveLength(1)
    expect(result.skyLayers[0].coverage).toBe('Few clouds')
    expect(result.skyLayers[0].altitude).toBe(25000)
    expect(result.temperature).toContain('-2')
    expect(result.dewpoint).toContain('-17')
    expect(result.altimeter).toContain('30.42')
    expect(result.remarks).toContain('AO2')
  })

  it('parses AUTO observations', () => {
    const raw = 'KORD 121853Z AUTO 28012KT 10SM CLR 01/M10 A3038'
    const result = parseMetar(raw)

    expect(result.station).toBe('KORD')
    expect(result.isAuto).toBe(true)
    expect(result.wind).toContain('280')
    expect(result.wind).toContain('12 knots')
    expect(result.skyLayers[0].coverage).toBe('Clear')
  })

  it('parses variable winds', () => {
    const raw = 'KSFO 121856Z VRB03KT 10SM SCT020 BKN040 15/10 A2992'
    const result = parseMetar(raw)

    expect(result.wind).toContain('Variable')
    expect(result.wind).toContain('3 knots')
    expect(result.skyLayers).toHaveLength(2)
    expect(result.skyLayers[0].coverage).toBe('Scattered clouds')
    expect(result.skyLayers[0].altitude).toBe(2000)
    expect(result.skyLayers[1].coverage).toBe('Broken clouds')
    expect(result.skyLayers[1].altitude).toBe(4000)
  })

  it('parses calm winds', () => {
    const raw = 'KATL 121856Z 00000KT 10SM OVC012 08/06 A3012'
    const result = parseMetar(raw)

    expect(result.wind).toBe('Calm')
    expect(result.skyLayers[0].coverage).toBe('Overcast')
  })

  it('parses weather phenomena', () => {
    const raw = 'KDEN 121856Z 18008KT 3SM -RA BR OVC008 05/04 A2998'
    const result = parseMetar(raw)

    expect(result.weather).toContain('Light rain')
    expect(result.weather).toContain('Mist')
    expect(result.visibility).toBe('3 miles')
  })

  it('parses heavy weather', () => {
    const raw = 'KMIA 121856Z 09015KT 1/2SM +TSRA FG VV005 22/22 A2980'
    const result = parseMetar(raw)

    expect(result.weather).toContain('Heavy thunderstorm rain')
    expect(result.weather).toContain('Fog')
    expect(result.visibility).toContain('1/2')
    expect(result.skyLayers[0].coverage).toBe('Vertical visibility')
    expect(result.skyLayers[0].altitude).toBe(500)
  })

  it('parses freezing precipitation', () => {
    const raw = 'KORD 121856Z 35010KT 2SM FZRA OVC010 M01/M02 A3001'
    const result = parseMetar(raw)

    expect(result.weather).toContain('Freezing rain')
  })

  it('parses negative temperatures', () => {
    const raw = 'PANC 121856Z 01012KT 10SM FEW060 M15/M20 A2985'
    const result = parseMetar(raw)

    expect(result.temperature).toContain('-15')
    expect(result.dewpoint).toContain('-20')
    expect(result.temperature).toContain('5°F')
  })

  it('parses low visibility', () => {
    const raw = 'KSFO 121856Z 18005KT M1/4SM FG VV001 10/10 A3000'
    const result = parseMetar(raw)

    expect(result.visibility).toBe('Less than 1/4 mile')
  })

  it('parses P6SM visibility', () => {
    const raw = 'KLAX 121856Z 25010KT P6SM SKC 18/08 A2995'
    const result = parseMetar(raw)

    expect(result.visibility).toBe('Greater than 6 miles')
    expect(result.skyLayers[0].coverage).toBe('Clear')
  })

  it('computes flight category VFR', () => {
    const raw = 'KJFK 121856Z 31015KT 10SM FEW250 02/M17 A3042'
    const result = parseMetar(raw)
    expect(result.flightCategory).toBe('VFR')
  })

  it('computes flight category IFR', () => {
    const raw = 'KDEN 121856Z 18008KT 2SM OVC008 05/04 A2998'
    const result = parseMetar(raw)
    expect(result.flightCategory).toBe('IFR')
  })

  it('computes flight category LIFR', () => {
    const raw = 'KSFO 121856Z 18005KT M1/4SM FG VV001 10/10 A3000'
    const result = parseMetar(raw)
    expect(result.flightCategory).toBe('LIFR')
  })

  it('parses CB cloud type', () => {
    const raw = 'KMIA 121856Z 09015KT 5SM SCT025CB BKN050 28/24 A2990'
    const result = parseMetar(raw)

    expect(result.skyLayers[0].cloudType).toBe('Cumulonimbus')
    expect(result.skyLayers[0].altitude).toBe(2500)
  })
})

describe('parseTaf', () => {
  it('parses a standard TAF', () => {
    const raw = 'TAF KJFK 121730Z 1218/1324 31015G25KT P6SM FEW250 FM130200 28008KT P6SM SCT080 TEMPO 1306/1310 3SM -RA BKN030'
    const result = parseTaf(raw)

    expect(result.station).toBe('KJFK')
    expect(result.forecasts.length).toBeGreaterThanOrEqual(3)

    // Base forecast
    expect(result.forecasts[0].type).toBe('BASE')
    expect(result.forecasts[0].wind).toContain('15 knots')
    expect(result.forecasts[0].visibility).toBe('Greater than 6 miles')

    // FM group
    const fm = result.forecasts.find(f => f.type === 'FM')
    expect(fm).toBeDefined()
    expect(fm!.wind).toContain('8 knots')

    // TEMPO group
    const tempo = result.forecasts.find(f => f.type === 'TEMPO')
    expect(tempo).toBeDefined()
    expect(tempo!.visibility).toContain('3')
    expect(tempo!.weather).toContain('Light rain')
    expect(tempo!.skyLayers[0].coverage).toBe('Broken clouds')
  })

  it('parses amended TAF', () => {
    const raw = 'TAF AMD KORD 121900Z 1219/1324 28012KT P6SM SCT250'
    const result = parseTaf(raw)

    expect(result.station).toBe('KORD')
    expect(result.forecasts).toHaveLength(1)
    expect(result.forecasts[0].type).toBe('BASE')
  })

  it('parses PROB groups', () => {
    const raw = 'TAF KSEA 121730Z 1218/1324 18008KT P6SM SCT040 PROB30 TEMPO 1302/1306 2SM -TSRA OVC020CB'
    const result = parseTaf(raw)

    const prob = result.forecasts.find(f => f.type === 'PROB')
    expect(prob).toBeDefined()
    expect(prob!.probability).toBe(30)
    expect(prob!.weather).toContain('Light thunderstorm rain')
    expect(prob!.skyLayers[0].cloudType).toBe('Cumulonimbus')
  })

  it('parses BECMG groups', () => {
    const raw = 'TAF KLAX 121730Z 1218/1324 25010KT P6SM SKC BECMG 1302/1304 18005KT SCT020'
    const result = parseTaf(raw)

    const becmg = result.forecasts.find(f => f.type === 'BECMG')
    expect(becmg).toBeDefined()
    expect(becmg!.wind).toContain('5 knots')
    expect(becmg!.skyLayers[0].coverage).toBe('Scattered clouds')
  })

  it('parses NSW (no significant weather)', () => {
    const raw = 'TAF KDEN 121730Z 1218/1324 35010KT 3SM -SN OVC010 FM130600 28008KT P6SM NSW SCT060'
    const result = parseTaf(raw)

    const fm = result.forecasts.find(f => f.type === 'FM')
    expect(fm).toBeDefined()
    expect(fm!.weather).toContain('No significant weather')
  })
})
