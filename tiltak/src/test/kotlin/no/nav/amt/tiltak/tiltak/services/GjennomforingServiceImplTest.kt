package no.nav.amt.tiltak.tiltak.services

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.amt.tiltak.core.domain.tiltak.Gjennomforing
import no.nav.amt.tiltak.core.domain.tiltak.GjennomforingUpsert
import no.nav.amt.tiltak.core.port.ArrangorService
import no.nav.amt.tiltak.core.port.TiltakService
import no.nav.amt.tiltak.data_publisher.DataPublisherService
import no.nav.amt.tiltak.test.database.DbTestDataUtils
import no.nav.amt.tiltak.test.database.SingletonPostgresContainer
import no.nav.amt.tiltak.test.database.data.TestData.ARRANGOR_1
import no.nav.amt.tiltak.test.database.data.TestData.ARRANGOR_2
import no.nav.amt.tiltak.test.database.data.TestData.GJENNOMFORING_1
import no.nav.amt.tiltak.test.database.data.TestData.NAV_ENHET_1
import no.nav.amt.tiltak.test.database.data.TestData.TILTAK_1
import no.nav.amt.tiltak.test.database.data.TestDataRepository
import no.nav.amt.tiltak.tiltak.repositories.GjennomforingRepository
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.util.UUID

class GjennomforingServiceImplTest : FunSpec({

	val dataSource = SingletonPostgresContainer.getDataSource()

	lateinit var gjennomforingRepository: GjennomforingRepository

	lateinit var testDataRepository: TestDataRepository

	lateinit var arrangorService: ArrangorService

	lateinit var tiltakService: TiltakService

	lateinit var service: GjennomforingServiceImpl

	lateinit var publisherService: DataPublisherService


	beforeEach {
		val parameterTemplate = NamedParameterJdbcTemplate(dataSource)

		gjennomforingRepository = GjennomforingRepository(parameterTemplate)

		testDataRepository = TestDataRepository(parameterTemplate)

		arrangorService = mockk()

		tiltakService = mockk()

		publisherService = mockk()


		service = GjennomforingServiceImpl(
			gjennomforingRepository = gjennomforingRepository,
			tiltakService = tiltakService,
			arrangorService = arrangorService,
			publisherService = publisherService
		)

		every { publisherService.publish(id = any(), type = any()) } returns Unit
		DbTestDataUtils.cleanDatabase(dataSource)
	}


	test("getGjennomforing - gjennomføring er kurs - returnerer gjennomføring") {
		val gjennomforingId = UUID.randomUUID()
		testDataRepository.insertNavEnhet(NAV_ENHET_1)
		testDataRepository.insertTiltak(TILTAK_1)
		testDataRepository.insertArrangor(ARRANGOR_1)
		testDataRepository.insertGjennomforing(GJENNOMFORING_1.copy(id=gjennomforingId, erKurs = true))

		val tiltakInserted = TILTAK_1.toTiltak()
		val arrangorInserted = ARRANGOR_1.toArrangor()

		every { arrangorService.getArrangorById(ARRANGOR_1.id) } returns arrangorInserted
		every { tiltakService.getTiltakById(TILTAK_1.id) } returns tiltakInserted
		service.getGjennomforing(gjennomforingId).id shouldBe gjennomforingId

	}

	test("getByLopenummer - returnerer alle gjennomføringer, uansett status") {
		testDataRepository.insertNavEnhet(NAV_ENHET_1)
		testDataRepository.insertTiltak(TILTAK_1)
		testDataRepository.insertArrangor(ARRANGOR_1)
		testDataRepository.insertGjennomforing(GJENNOMFORING_1)

		val avsluttetGjennomforing = GJENNOMFORING_1.copy(id = UUID.randomUUID(), status = Gjennomforing.Status.AVSLUTTET.name)
		testDataRepository.insertGjennomforing(avsluttetGjennomforing)

		val tiltakInserted = TILTAK_1.toTiltak()
		val arrangorInserted = ARRANGOR_1.toArrangor()
		val lopenr = GJENNOMFORING_1.lopenr

		every { arrangorService.getArrangorById(ARRANGOR_1.id) } returns arrangorInserted
		every { tiltakService.getTiltakById(TILTAK_1.id) } returns tiltakInserted

		val expectedIds = listOf(GJENNOMFORING_1.id, avsluttetGjennomforing.id)

		service.getByLopenr(lopenr).map { it.id } shouldContainAll  expectedIds
	}

	test("upsert - arrangørId er endret - arrangørId oppdateres for gjennomføringen") {
		testDataRepository.insertNavEnhet(NAV_ENHET_1)
		testDataRepository.insertTiltak(TILTAK_1)
		testDataRepository.insertArrangor(ARRANGOR_1)
		testDataRepository.insertArrangor(ARRANGOR_2)
		testDataRepository.insertGjennomforing(GJENNOMFORING_1)
		val tiltakInserted = TILTAK_1.toTiltak()
		val oppdatertArrangor = ARRANGOR_2.toArrangor()

		every { arrangorService.getArrangorById(ARRANGOR_2.id) } returns oppdatertArrangor
		every { tiltakService.getTiltakById(TILTAK_1.id) } returns tiltakInserted

		service.upsert(
			GjennomforingUpsert(
				id = GJENNOMFORING_1.id,
				tiltakId = TILTAK_1.id,
				arrangorId = ARRANGOR_2.id,
				navn = GJENNOMFORING_1.navn,
				status = Gjennomforing.Status.GJENNOMFORES,
				startDato = GJENNOMFORING_1.startDato,
				sluttDato = GJENNOMFORING_1.sluttDato,
				navEnhetId = NAV_ENHET_1.id,
				lopenr = GJENNOMFORING_1.lopenr,
				opprettetAar = GJENNOMFORING_1.opprettetAar,
				erKurs = false
			)
		)

		val oppdatertGjennomforing = service.getGjennomforing(GJENNOMFORING_1.id)
		oppdatertGjennomforing.arrangor.id shouldBe ARRANGOR_2.id
	}

})
