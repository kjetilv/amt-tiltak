package no.nav.amt.tiltak.core.domain.tiltak

import java.time.LocalDate
import java.util.*

class Deltaker(
	val id: UUID,
	val fornavn: String,
	val etternavn: String,
	val fodselsnummer: String,
	val oppstartdato: LocalDate?,
	val sluttdato: LocalDate?,
	val status: Status
) {

	enum class Status {
		VENTER_PA_OPPSTART, GJENNOMFORES, HAR_SLUTTET, IKKE_AKTUELL
	}

}

