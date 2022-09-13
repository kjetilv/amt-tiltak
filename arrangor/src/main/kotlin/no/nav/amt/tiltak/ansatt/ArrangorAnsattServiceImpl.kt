package no.nav.amt.tiltak.ansatt

import no.nav.amt.tiltak.core.domain.arrangor.Ansatt
import no.nav.amt.tiltak.core.domain.arrangor.TilknyttetArrangor
import no.nav.amt.tiltak.core.port.ArrangorAnsattService
import no.nav.amt.tiltak.core.port.ArrangorAnsattTilgangService
import no.nav.amt.tiltak.core.port.ArrangorService
import no.nav.amt.tiltak.core.port.PersonService
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import java.util.*

@Service
class ArrangorAnsattServiceImpl(
	private val arrangorAnsattRepository: ArrangorAnsattRepository,
	private val personService: PersonService,
	private val arrangorAnsattTilgangService: ArrangorAnsattTilgangService,
	private val arrangorService: ArrangorService,
) : ArrangorAnsattService {

	override fun opprettAnsattHvisIkkeFinnes(personIdent: String): Ansatt {
		return getAnsattByPersonligIdent(personIdent)
			?: createAnsatt(personIdent)
	}

	override fun getAnsatt(ansattId: UUID): Ansatt {
		val ansattDbo = arrangorAnsattRepository.get(ansattId) ?: throw NoSuchElementException("Ansatt ikke funnet")
		val arrangorer = hentTilknyttedeArrangorer(ansattId)

		return ansattDbo.toAnsatt(arrangorer)
	}

	override fun getAnsattByPersonligIdent(personIdent: String): Ansatt? {
		val ansattDbo = arrangorAnsattRepository.getByPersonligIdent(personIdent) ?: return null
		val arrangorer = hentTilknyttedeArrangorer(ansattDbo.id)

		return ansattDbo.toAnsatt(arrangorer)
	}

	private fun createAnsatt(ansattPersonIdent: String): Ansatt {
		val person = personService.hentPerson(ansattPersonIdent)

		val nyAnsattId = UUID.randomUUID()

		arrangorAnsattRepository.opprettAnsatt(
			id = nyAnsattId,
			personligIdent = ansattPersonIdent,
			fornavn = person.fornavn,
			mellomnavn = person.mellomnavn,
			etternavn = person.etternavn
		)

		return getAnsatt(nyAnsattId)
	}

	private fun hentTilknyttedeArrangorer(ansattId: UUID): List<TilknyttetArrangor> {
		val roller = arrangorAnsattTilgangService.hentAnsattTilganger(ansattId)
		val arrangorIder = roller.map { it.arrangorId }

		return arrangorService.getArrangorerById(arrangorIder).map {
			val arrangorRoller = roller.find { r -> r.arrangorId == it.id }
				?: throw IllegalStateException("Fant ikke roller")
			return@map TilknyttetArrangor(
				id = it.id,
				navn = it.navn,
				organisasjonsnummer = it.organisasjonsnummer,
				overordnetEnhetOrganisasjonsnummer = it.overordnetEnhetOrganisasjonsnummer,
				overordnetEnhetNavn = it.overordnetEnhetNavn,
				roller = arrangorRoller.roller.map { it.name }
			)
		}
	}
}
