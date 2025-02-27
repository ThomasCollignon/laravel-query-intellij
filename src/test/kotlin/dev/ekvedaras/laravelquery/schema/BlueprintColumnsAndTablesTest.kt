package dev.ekvedaras.laravelquery.schema

import com.intellij.database.model.ObjectKind
import com.intellij.database.util.DasUtil
import com.intellij.database.util.DbImplUtil
import com.intellij.database.util.DbUtil
import com.intellij.testFramework.UsefulTestCase
import dev.ekvedaras.laravelquery.BaseTestCase
import dev.ekvedaras.laravelquery.inspection.UnknownColumnInspection
import dev.ekvedaras.laravelquery.inspection.UnknownTableOrViewInspection
import dev.ekvedaras.laravelquery.reference.ColumnPsiReference
import dev.ekvedaras.laravelquery.reference.TableOrViewPsiReference
import junit.framework.TestCase

internal class BlueprintColumnsAndTablesTest : BaseTestCase() {
    fun testCompletesColumnsForRename() {
        val table = DasUtil.getTables(dataSource()).first { it.name == "users" }

        myFixture.configureByFile("schema/renameColumn.php")
        myFixture.completeBasic()

        assertCompletion(*table.getDasChildren(ObjectKind.COLUMN).map { it.name }.toList().toTypedArray())
        assertNoCompletion(*schemas.toTypedArray())
        assertNoCompletion(*schemaTables.values.flatten().toTypedArray())
    }

    fun testCompletesColumnsForUniqueIndex() {
        val table = DasUtil.getTables(dataSource()).first { it.name == "users" }

        myFixture.configureByFile("schema/unique.php")
        myFixture.completeBasic()

        assertCompletion(*table.getDasChildren(ObjectKind.COLUMN).map { it.name }.toList().toTypedArray())
        assertNoCompletion(*schemas.toTypedArray())
        assertNoCompletion(*schemaTables.values.flatten().toTypedArray())
    }

    fun testCompletesColumnsForAfter() {
        val table = DasUtil.getTables(dataSource()).first { it.name == "users" }

        myFixture.configureByFile("schema/after.php")
        myFixture.completeBasic()

        assertCompletion(*table.getDasChildren(ObjectKind.COLUMN).map { it.name }.toList().toTypedArray())
        assertNoCompletion(*schemas.toTypedArray())
        assertNoCompletion(*schemaTables.values.flatten().toTypedArray())
    }

    fun testResolvesStringColumnReference() {
        myFixture.configureByFile("schema/string.php")

        val table = DasUtil.getTables(dataSource()).first { it.name == "users" }
        val column = table.getDasChildren(ObjectKind.COLUMN).first { it.name == "email" }
        val dbColumn = DbImplUtil.findElement(DbUtil.getDataSources(project).first(), column)
            ?: return fail("Failed to resolve DB column")

        val usages = myFixture.findUsages(dbColumn)

        UsefulTestCase.assertSize(1, usages)
        TestCase.assertEquals(ColumnPsiReference::class.java, usages.first().referenceClass)
        TestCase.assertTrue(usages.first().element?.textMatches("'email'") ?: false)
        TestCase.assertEquals(107, usages.first().navigationRange.startOffset)
        TestCase.assertEquals(107 + column.name.length, usages.first().navigationRange.endOffset)
    }

    fun testResolvesTableReference() {
        myFixture.configureByFile("schema/rename.php")

        val table = DasUtil.getTables(dataSource()).first { it.name == "failed_jobs" }
        val dbTable = DbImplUtil.findElement(DbUtil.getDataSources(project).first(), table)
            ?: return fail("Failed to resolve DB table")

        val usages = myFixture.findUsages(dbTable)

        UsefulTestCase.assertSize(1, usages)
        TestCase.assertEquals(TableOrViewPsiReference::class.java, usages.first().referenceClass)
        TestCase.assertTrue(usages.first().element?.textMatches("'failed_jobs'") ?: false)
        TestCase.assertEquals(107, usages.first().navigationRange.startOffset)
        TestCase.assertEquals(107 + table.name.length, usages.first().navigationRange.endOffset)
    }

    fun testDoesNotWarnAboutUnknownColumn() {
        assertInspection("schema/unknownColumn.php", UnknownColumnInspection())
    }

    fun testDoesNotWarnAboutUnknownTable() {
        assertInspection("schema/unknownBlueprintTable.php", UnknownTableOrViewInspection())
    }
}
