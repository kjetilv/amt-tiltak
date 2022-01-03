package no.nav.amt.tiltak.tiltak.repositories

import no.nav.amt.tiltak.core.domain.tiltak.Gjennomforing
import no.nav.amt.tiltak.tiltak.dbo.GjennomforingDbo
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@Component
open class GjennomforingRepository(private val template: NamedParameterJdbcTemplate) {

	private val rowMapper = RowMapper { rs, _ ->
		val statusString = rs.getString("status")

		GjennomforingDbo(
			id = UUID.fromString(rs.getString("id")),
			arenaId = rs.getInt("arena_id"),
			arrangorId = UUID.fromString(rs.getString("arrangor_id")),
			tiltakId = UUID.fromString(rs.getString("tiltak_id")),
			navn = rs.getString("navn"),
			status = if (statusString != null) Gjennomforing.Status.valueOf(statusString) else null,
			oppstartDato = rs.getDate("oppstart_dato")?.toLocalDate(),
			sluttDato = rs.getDate("slutt_dato")?.toLocalDate(),
			registrertDato = rs.getTimestamp("registrert_dato")?.toLocalDateTime(),
			fremmoteDato = rs.getTimestamp("fremmote_dato")?.toLocalDateTime(),
			createdAt = rs.getTimestamp("created_at").toLocalDateTime(),
			modifiedAt = rs.getTimestamp("modified_at").toLocalDateTime()
		)
	}

	fun insert(
		arenaId: Int,
		tiltakId: UUID,
		arrangorId: UUID,
		navn: String,
		status: Gjennomforing.Status?,
		oppstartDato: LocalDate?,
		sluttDato: LocalDate?,
		registrertDato: LocalDateTime?,
		fremmoteDato: LocalDateTime?
	): GjennomforingDbo {

		//language=PostgreSQL
		val sql = """
		INSERT INTO gjennomforing(id, arena_id, tiltak_id, arrangor_id, navn, status, oppstart_dato,
                           slutt_dato, registrert_dato, fremmote_dato)
		VALUES (:id,
				:arenaId,
				:tiltakId,
				:arrangorId,
				:navn,
				:status,
				:oppstartDato,
				:sluttDato,
				:registrertDato,
				:fremmoteDato)
	""".trimIndent()

		val id = UUID.randomUUID()

		val parameters = MapSqlParameterSource().addValues(
			mapOf(
				"id" to id,
				"arenaId" to arenaId,
				"tiltakId" to tiltakId,
				"arrangorId" to arrangorId,
				"navn" to navn,
				"status" to status?.name,
				"oppstartDato" to oppstartDato,
				"sluttDato" to sluttDato,
				"registrertDato" to registrertDato,
				"fremmoteDato" to fremmoteDato
			)
		)

		template.update(sql, parameters)

		return get(id)
			?: throw NoSuchElementException("Tiltak med id $id finnes ikke")
	}

	fun update(gjennomforing: GjennomforingDbo): GjennomforingDbo {

		//language=PostgreSQL
		val sql = """
			UPDATE gjennomforing
			SET navn            = :navn,
				status          = :status,
				oppstart_dato   = :oppstart_dato,
				slutt_dato      = :slutt_dato,
				registrert_dato = :registrert_dato,
				fremmote_dato   = :fremmote_dato,
				modified_at     = :modified_at
			WHERE id = :id
		""".trimIndent()

		val parameters = MapSqlParameterSource().addValues(
			mapOf(
				"navn" to gjennomforing.navn,
				"status" to gjennomforing.status?.name,
				"oppstart_dato" to gjennomforing.oppstartDato,
				"slutt_dato" to gjennomforing.sluttDato,
				"registrert_dato" to gjennomforing.registrertDato,
				"fremmote_dato" to gjennomforing.fremmoteDato,
				"modified_at" to gjennomforing.modifiedAt,
				"id" to gjennomforing.id
			)
		)

		template.update(sql, parameters)

		return get(gjennomforing.id)
			?: throw NoSuchElementException("Tiltak med id ${gjennomforing.id} finnes ikke")
	}


	fun get(id: UUID): GjennomforingDbo? {

		//language=PostgreSQL
		val sql = """
			SELECT *
			FROM gjennomforing WHERE id = :id
		""".trimIndent()

		val parameters = MapSqlParameterSource().addValues(
			mapOf(
				"id" to id
			)
		)

		return template.query(sql, parameters, rowMapper).firstOrNull()
	}

	fun getByArenaId(arenaId: Int): GjennomforingDbo? {

		//language=PostgreSQL
		val sql = """
			SELECT *
			FROM gjennomforing
			WHERE arena_id = :arenaId
		""".trimIndent()

		val parameters = MapSqlParameterSource().addValues(
			mapOf(
				"arenaId" to arenaId
			)
		)

		return template.query(sql, parameters, rowMapper).firstOrNull()
	}

	fun getByArrandorId(arrangorId: UUID): List<GjennomforingDbo> {

		//language=PostgreSQL
		val sql = """
			SELECT *
			FROM gjennomforing
			WHERE arrangor_id = :arrangorId
		""".trimIndent()

		val parameters = MapSqlParameterSource().addValues(
			mapOf(
				"arrangorId" to arrangorId
			)
		)

		return template.query(sql, parameters, rowMapper)
	}

}
