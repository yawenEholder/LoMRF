/*
 * o                        o     o   o         o
 * |             o          |     |\ /|         | /
 * |    o-o o--o    o-o  oo |     | O |  oo o-o OO   o-o o   o
 * |    | | |  | | |    | | |     |   | | | |   | \  | |  \ /
 * O---oo-o o--O |  o-o o-o-o     o   o o-o-o   o  o o-o   o
 *             |
 *          o--o
 * o--o              o               o--o       o    o
 * |   |             |               |    o     |    |
 * O-Oo   oo o-o   o-O o-o o-O-o     O-o    o-o |  o-O o-o
 * |  \  | | |  | |  | | | | | |     |    | |-' | |  |  \
 * o   o o-o-o  o  o-o o-o o o o     o    | o-o o  o-o o-o
 *
 * Logical Markov Random Fields.
 *
 * Copyright (C) 2012  Anastasios Skarlatidis.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package lomrf.logic

import org.scalatest.{FunSpec, Matchers}
import org.junit.Assert._
import lomrf.mln.model.MLN
import lomrf.util.ConstantsSet

/**
 * @author Anastasios Skarlatidis
 */

final class NormalFormSpecTest extends FunSpec with Matchers{

  private val sep = System.getProperty("file.separator")
  private val testFilesPath = System.getProperty("user.dir") + sep + "Examples" + sep + "data" + sep + "tests" + sep

  private val constants = Map[String, ConstantsSet](
    "time" -> ConstantsSet("1", "2", "3", "4"),
    "event" -> ConstantsSet("Abrupt", "Walking", "Running", "Active", "Inactive", "Exit"),
    "fluent" -> ConstantsSet("Fight", "Move", "Meet", "Leaving_object"),
    "dist" -> ConstantsSet("24", "30", "35"),
    "id" -> ConstantsSet("ID1", "ID2", "ID3"),

    "a" -> ConstantsSet((1 to 4).map(_.toString): _*),
    "b" -> ConstantsSet("B1", "B2")
  )

  private val predicateSchema = Map[AtomSignature, Vector[String]](
    AtomSignature("InitiatedAt", 2) -> Vector("fluent", "time"),
    AtomSignature("TerminatedAt", 2) -> Vector("fluent", "time"),
    AtomSignature("Initiates", 3) -> Vector("event", "fluent", "time"),
    AtomSignature("Terminates", 3) -> Vector("event", "fluent", "time"),
    AtomSignature("Happens", 2) -> Vector("event", "time"),
    AtomSignature("HoldsAt", 2) -> Vector("fluent", "time"),
    AtomSignature("Next", 2) -> Vector("time", "time"),
    AtomSignature("Close", 2) -> Vector("dist", "time"),
    AtomSignature("Close", 4) -> Vector("id", "id", "dist", "time"),
    AtomSignature("OrientationMove", 1) -> Vector("time"),
    AtomSignature("StartedAt", 3) -> Vector("event", "fluent", "time"),
    AtomSignature("StoppedAt", 3) -> Vector("event", "fluent", "time"),

    AtomSignature("Predicate1", 1) -> Vector("a"),
    AtomSignature("Predicate2", 2) -> Vector("a", "b"),
    AtomSignature("Predicate3", 1) -> Vector("b")
  )

  private val functionsSchema = Map[AtomSignature, (String, Vector[String])](
    AtomSignature("walking", 1) ->("event", Vector("id")),
    AtomSignature("abrupt", 1) ->("event", Vector("id")),
    AtomSignature("running", 1) ->("event", Vector("id")),
    AtomSignature("active", 1) ->("event", Vector("id")),
    AtomSignature("inactive", 1) ->("event", Vector("id")),
    AtomSignature("exit", 1) ->("event", Vector("id")),
    AtomSignature("fight", 2) ->("fluent", Vector("id", "id")),
    AtomSignature("move", 2) ->("fluent", Vector("id", "id")),
    AtomSignature("meet", 2) ->("fluent", Vector("id", "id")),
    AtomSignature("leaving_object", 2) ->("fluent", Vector("id", "id"))
  )


  private val kbParser = new KBParser(predicateSchema, functionsSchema)


  describe("Formula 'InitiatedAt(Fight,t) => Happens(Abrupt, t).'"){
    val formula = kbParser.parseFormula("InitiatedAt(Fight,t) => Happens(Abrupt, t).")
    val clauses = NormalForm.toCNF(constants, formula)

    it("results to a single valid clause"){
      clauses.size shouldEqual 1
      val literals = clauses.head.literals
      assertEquals(2, literals.size)
      assertTrue(literals.contains(kbParser.parseLiteral("!InitiatedAt(Fight,t)")))
      assertTrue(literals.contains(kbParser.parseLiteral("Happens(Abrupt, t)")))
    }
  }



  describe("Formula 'InitiatedAt(Fight,t) <=> Happens(Abrupt, t).'"){
    val formula = kbParser.parseFormula("InitiatedAt(Fight,t) <=> Happens(Abrupt, t).")
    val clauses = NormalForm.toCNF(constants, formula)

    it("produces two valid clauses"){
      clauses.size shouldEqual 2
      assertContainsClause(clauses, "{!InitiatedAt(Fight,t) | Happens(Abrupt, t)}")
      assertContainsClause(clauses, "{InitiatedAt(Fight,t) | !Happens(Abrupt, t)}")
    }
  }


  describe("Formula 'InitiatedAt(Fight,t) <=> Happens(Abrupt, t) ^ Close(10,t).'"){
    val f0 = kbParser.parseFormula("InitiatedAt(Fight,t) <=> Happens(Abrupt, t) ^ Close(10,t).")
    val clauses0 = f0.toCNF(constants)

    it("produces three valid clauses"){
      assertEquals(3, clauses0.size)
      assertContainsClause(clauses0, "{InitiatedAt(Fight, t) | !Happens(Abrupt, t) | !Close(10,t)}")
      assertContainsClause(clauses0, "{Happens(Abrupt,t) | !InitiatedAt(Fight, t)}")
      assertContainsClause(clauses0, "{Close(10,t) | !InitiatedAt(Fight, t)}")
    }
  }

  describe("The formula 'Happens(walking(id1),t) => Initiates(walking(id1), move(id,id2), t).'"){
    val f = kbParser.parseFormula("Happens(walking(id1),t) => Initiates(walking(id1), move(id,id2), t).")
    val clauses = f.toCNF(constants)

    it("produces a single clause"){
      assert(clauses.size == 1)
    }
  }

  describe("The formulas:\n" +
    "\t 'InitiatedAt(Fight,t) => Happens(Abrupt, t) ^ Close(10,t).' and\n" +
    "\t 'Happens(Abrupt, t) ^ Close(10,t) => InitiatedAt(Fight,t).'") {

    val f1 = kbParser.parseFormula("InitiatedAt(Fight,t) => Happens(Abrupt, t) ^ Close(10,t).")
    val f2 = kbParser.parseFormula("Happens(Abrupt, t) ^ Close(10,t) => InitiatedAt(Fight,t).")
    val clauses_1and2 = f1.toCNF(constants) ++ f2.toCNF(constants)

    they("produce three valid clauses"){
      assertEquals(3, clauses_1and2.size)

      assertContainsClause(clauses_1and2, "{InitiatedAt(Fight, t) | !Happens(Abrupt, t) | !Close(10,t)}")
      assertContainsClause(clauses_1and2, "{Happens(Abrupt,t) | !InitiatedAt(Fight, t)}")
      assertContainsClause(clauses_1and2, "{Close(10,t) | !InitiatedAt(Fight, t)}")

    }

    they("produce the same clauses with the formula 'InitiatedAt(Fight,t) <=> Happens(Abrupt, t) ^ Close(10,t).'"){
      val f0 = kbParser.parseFormula("InitiatedAt(Fight,t) <=> Happens(Abrupt, t) ^ Close(10,t).")
      val clauses0 = f0.toCNF(constants)
      clauses_1and2.foreach(c => clauses0.contains(c) shouldEqual true)
    }
  }

  describe("The weighted formulas:\n" +
    "\t '1 Happens(Abrupt, t) ^ Close(24,t) ^ !Happens(Inactive,t ) => InitiatedAt(Fight, t)' and\n" +
    "\t '3 InitiatedAt(Fight,t) => Happens(Abrupt, t) ^ !Happens(Inactive, t) ^ Close(24,t)'"){

    val f0 = kbParser.parseFormula("1 Happens(Abrupt, t) ^ Close(24,t) ^ !Happens(Inactive,t ) => InitiatedAt(Fight, t)")
    val f1 = kbParser.parseFormula("3 InitiatedAt(Fight,t) => Happens(Abrupt, t) ^ !Happens(Inactive, t) ^ Close(24,t)")

    val clauses01 = f0.toCNF(constants) ++ f1.toCNF(constants)


    they("produce four clauses"){
      clauses01.size shouldEqual 4
    }

    they("produce the same clauses with the weighed formula '4 Happens(Abrupt, t) ^ Close(24,t) ^ !Happens(Inactive,t ) <=> InitiatedAt(Fight, t)'"){
      val f3 = kbParser.parseFormula("4 Happens(Abrupt, t) ^ Close(24,t) ^ !Happens(Inactive,t ) <=> InitiatedAt(Fight, t)")
      val clauses3 = f3.toCNF(constants)

      clauses3.size shouldEqual 4
      clauses01.foreach(c => clauses3.contains(c) shouldEqual true)
    }
  }


  describe("Formula 'Exist x,t Happens(walking(x), t).'"){
    val f = kbParser.parseFormula("Exist x,t Happens(walking(x), t).")
    val clauses = f.toCNF(constants)

    it("produces a single clause"){
      assert(clauses.size == 1)
    }

    it("has 12 literals"){
      assert(clauses.head.literals.size == 12)
    }
  }

  describe("The weighted formulas: \n" +
    "\t1 Happens(abrupt(p1), t) ^ Close(p1,p2,24,t) ^ !Happens(inactive(p2),t ) => InitiatedAt(fight(p1,p2), t) and" +
    "\t3 InitiatedAt(fight(p1,p2),t) => Happens(abrupt(p1), t) ^ !Happens(inactive(p2), t) ^ Close(p1,p2,24,t)") {

    val f0 = kbParser.parseFormula("1 Happens(abrupt(p1), t) ^ Close(p1,p2,24,t) ^ !Happens(inactive(p2),t ) => InitiatedAt(fight(p1,p2), t)")
    val f1 = kbParser.parseFormula("3 InitiatedAt(fight(p1,p2),t) => Happens(abrupt(p1), t) ^ !Happens(inactive(p2), t) ^ Close(p1,p2,24,t)")
    val clauses01 = f0.toCNF(constants) ++ f1.toCNF(constants)

    they("produce four clauses") {
      assert(clauses01.size == 4)
    }

    they("produce the same clauses with formula '4 Happens(abrupt(p1), t) ^ Close(p1,p2,24,t) ^ !Happens(inactive(p2),t ) <=> InitiatedAt(fight(p1,p2), t)'") {
      val f3 = kbParser.parseFormula("4 Happens(abrupt(p1), t) ^ Close(p1,p2,24,t) ^ !Happens(inactive(p2),t ) <=> InitiatedAt(fight(p1,p2), t)")
      val clauses3 = f3.toCNF(constants)
      assert(clauses3.size == 4)
      clauses01.foreach(c => assert(clauses3.contains(c)))
    }

  }

  describe("Test axiom from Discrete Event Calculus dialects:"){
    val constants = Map[String, ConstantsSet](
      "time" -> ConstantsSet("1", "2", "3", "4"),
      "event" -> ConstantsSet("Abrupt", "Walking", "Running", "Active", "Inactive", "Exit"),
      "fluent" -> ConstantsSet("Fight", "Move", "Meet", "Leaving_object"),
      "dist" -> ConstantsSet("24", "30", "35")
    )

    val decTests = List(
      //DEC v1
      ("Next(t1,t0) ^ Happens(e,t0) ^ Initiates(e,f,t0) => HoldsAt(f,t1).", 1),
      ("Next(t1,t0) ^ Happens(e,t0) ^ Terminates(e,f,t0) => !HoldsAt(f,t1).",1),
      ("Next(t1,t0) ^ HoldsAt(f,t0) ^ !(Exist e Happens(e,t0) ^ Terminates(e,f,t0) ) => HoldsAt(f,t1).", 64),
      ("Next(t1,t0) ^ !HoldsAt(f,t0) ^ !(Exist e Happens(e,t0) ^ Initiates(e,f,t0) ) => !HoldsAt(f,t1).", 64),
      //DEC v2
      ("Happens(e,t0) ^ Initiates(e,f,t0) => StartedAt(e,f,t0).", 1),
      ("Happens(e,t0) ^ Terminates(e,f,t0) => StoppedAt(e,f,t0).", 1),
      ("Next(t1,t0) ^ StartedAt(e,f,t0) => HoldsAt(f,t1).", 1),
      ("Next(t1,t0) ^ StoppedAt(e,f,t0) => !HoldsAt(f,t1).", 1),
      ("Next(t1,t0) ^ HoldsAt(f,t0) ^ !(Exist e StoppedAt(e,f,t0) ) => HoldsAt(f,t1).", 1),
      ("Next(t1,t0) ^ !HoldsAt(f,t0) ^ !(Exist e StartedAt(e,f,t0) ) => !HoldsAt(f,t1).", 1),
      //DEC v3
      ("Next(t1,t0) ^ InitiatedAt(f,t0) => HoldsAt(f,t1).", 1),
      ("Next(t1,t0) ^ TerminatedAt(f,t0) => !HoldsAt(f,t1).", 1),
      ("Next(t1,t0) ^ HoldsAt(f,t0) ^ !TerminatedAt(f,t0) => HoldsAt(f,t1).", 1),
      ("Next(t1,t0) ^ !HoldsAt(f,t0) ^ !InitiatedAt(f,t0) => !HoldsAt(f,t1).", 1)
    )

    for((axiom, numClauses) <- decTests) describe("Axiom '"+axiom+"'"){
      val dec = kbParser.parseFormula(axiom)
      it("produces "+numClauses+" clause(s)"){
        val clauses = dec.toCNF(constants)
        assert(clauses.size == numClauses)
      }
    }

  }

  describe("The MLN from file '"+testFilesPath + "DEC.mln' with evidence from file '"+testFilesPath + "DEC.db'"){

    val mln = MLN(
      mlnFileName = testFilesPath + "DEC.mln",
      evidenceFileName = testFilesPath + "DEC.db",
      queryAtoms = Set(AtomSignature("HoldsAt", 2)),
      cwa = Set(AtomSignature("Happens", 2), AtomSignature("Close", 4), AtomSignature("Next", 2)),
      owa = Set(AtomSignature("Initiates", 3), AtomSignature("Terminates", 3), AtomSignature("StartsAt", 3), AtomSignature("StopsAt", 3)))

    info("Found " + mln.formulas.size + " formulas")
    info("Found " + mln.constants.size + " constant types")
    info("Found " + mln.predicateSchema.size + " predicate schemas")
    info("Found " + mln.functionSchema.size + " function schemas")

    it("should contain 14 formulas"){
      mln.formulas.size should be (14)

      for {
        f <- mln.formulas
        clauses = f.toCNF(constants)} {
        clInfo("Formula '" + f.toText + "'", clauses)
        assert(clauses.size > 0)
      }
    }

    it("should constants 5 constants sets (domains)"){
      mln.constants.size should be (5)
    }

    it("should contain 8 predicate schemas"){
      mln.predicateSchema.size should be (8)
    }

    it("should contain 11 function schemas"){
      mln.functionSchema.size should be (11)
    }
  }

  describe("Formula 'InitiatedAt(f,t) => !(Happens(e, t) ^ Exist x Close(x,t))'"){
    val src = "InitiatedAt(f,t) => !(Happens(e, t) ^ Exist x Close(x,t))"
    val formula = kbParser.parseFormula(src)

    it("should produce the NNF clause '!InitiatedAt(f,t) v !Happens(e,t) v (Forall x !Close(x,t))'"){
      val nnf = NormalForm.toNNF(formula)
      assert(nnf.toText == "!InitiatedAt(f,t) v !Happens(e,t) v (Forall x !Close(x,t))")
    }
  }

  describe("Formula 'InitiatedAt(f,t) => !(Happens(e, t) ^ Exist x Close(x,t))'"){
    val src = "InitiatedAt(f,t) => !(Happens(e, t) ^ Exist x Close(x,t))"
    val formula = kbParser.parseFormula(src)

    it("should produce the PNF clause '(Forall x !InitiatedAt(f,t) v !Happens(e,t) v !Close(x,t))'") {
      val pnf = NormalForm.toPNF(formula)
      assert(pnf.toText == "(Forall x !InitiatedAt(f,t) v !Happens(e,t) v !Close(x,t))")
    }
  }

  describe("The formula 'InitiatedAt(f,t) => HoldsAt(f, succ(t)).'"){
    val dec1 = kbParser.parseFormula("InitiatedAt(f,t) => HoldsAt(f, succ(t)).")

    it("contains the dynamic function succ(int):int"){
      assert(dec1.functions.size == 1)
      val f = dec1.functions.head
      assert(f.symbol == "succ")
      assert(f.domain == "time")
      assert(f.terms.size == 1)

      f.terms.head match{
        case v: Variable => assert(v.domain == "time")
        case _ => sys.error("The argument of function " + f + " should be a variable.")
      }
    }
  }


  private def assertContainsClause(clauses: Iterable[Clause], src: String) {
    val txtLiterals = src.replace("{", " ").replace("}", " ").split('|')
    var literals = Set[Literal]()

    for (lit <- txtLiterals) literals += kbParser.parseLiteral(lit)
    assertTrue {
      clauses.find(c => c.literals == literals) match {
        case Some(_) => true
        case _ => false
      }
    }
  }

  private def clInfo(decName: String, clauses: Iterable[Clause]) {
    info(decName + " produced the following " + clauses.size + " clauses:\n"
      + clauses.foldRight("")((clause, txt) => clause.toString + "\n" + txt))
  }
}