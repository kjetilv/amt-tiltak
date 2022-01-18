package no.nav.amt.tiltak.core.domain.tiltak

import java.util.*

data class Bruker(
	val id: UUID,
	val fornavn: String,
	val etternavn: String,
	val fodselsnummer: String
)
