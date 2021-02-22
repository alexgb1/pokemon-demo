package org.pokemon

import cats.effect.{ExitCode, IO, IOApp, Sync}
import sttp.tapir._
import sttp.tapir.server.http4s.Http4sServerInterpreter
import io.circe._
import io.circe.generic.auto._
import org.http4s.Uri
import org.http4s.client.Client
import org.http4s.circe._
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._
import org.http4s.circe.CirceEntityDecoder._
import cats.syntax.flatMap._
import cats.syntax.functor._
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.implicits.http4sLiteralsSyntax
import org.http4s.server.blaze.BlazeServerBuilder
import cats.syntax.either._

import scala.concurrent.ExecutionContext.global

object Server extends IOApp {

  case class Pokemon(description: String, imageUrl: String)

  val getPokemonEndpoint = endpoint.get
    .in("pokemon")
    .in(query[String]("name"))
    .out(jsonBody[Pokemon])


  // http client

  case class Sprites(front_default: String)

  case class Specie(name: String, url: String)

  case class Response(species: Specie, sprites: Sprites)

  case class Language(name: String)

  case class Flavor(flavor_text: String, language: Language)

  case class SpeciesResponse(flavor_text_entries: List[Flavor])

  trait PokemonRetrieve[F[_]] {
    def retrievePokemonUrl(name: String): F[Pokemon]
  }

  object PokemonRetrieve {
    def apply[F[_] : Sync](client: Client[F], uri: Uri): PokemonRetrieve[F] = new PokemonRetrieve[F] {
      override def retrievePokemonUrl(name: String): F[Pokemon] = {
        for {
          pokemonResponse <- client.expect[Response](uri / "pokemon" / name)
          description = Uri.unsafeFromString(pokemonResponse.species.url)
          speciesResponse <- client.expect[SpeciesResponse](description)
          description = speciesResponse.flavor_text_entries.find(_.language.name == "en").map(_.flavor_text).getOrElse("")
        } yield Pokemon(
          description = description,
          imageUrl = pokemonResponse.sprites.front_default
        )
      }
    }
  }


  override def run(args: List[String]): IO[ExitCode] = {
    for {
      pokemon <- BlazeClientBuilder[IO](global).resource.use(client => {
        val pokemonRetrieve = PokemonRetrieve[IO](client, uri"https://pokeapi.co/api/v2/")
        pokemonRetrieve.retrievePokemonUrl("pikachu")
      })
      _ <- IO(println(s"Pokemon: ${pokemon}"))
    } yield ExitCode.Success
  }

}
