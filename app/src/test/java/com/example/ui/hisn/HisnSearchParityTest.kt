package com.example.ui.hisn
import com.example.data.*
import org.junit.Assert.*
import org.junit.Test
class HisnSearchParityTest {
 @Test fun searchCoversWholeBookFromFavoritesAndRanksFavoritesFirst() {
  val chapters=listOf(HisnChapter(1,1,"أذكار الصباح"),HisnChapter(2,2,"أذكار المساء"),HisnChapter(3,3,"عند النوم"))
  val items=listOf(HisnItem(30,3,1,"الْحَمْدُ لِلَّهِ",1,"","",""))
  val base=HisnListState(chapters,emptyMap(),setOf(2),emptySet(),"",true)
  assertEquals(listOf(2),filterChapters(base,items).map{it.id})
  assertEquals(listOf(2,1),filterChapters(base.copy(query="اذكار"),items).map{it.id})
  assertEquals(listOf(3),filterChapters(base.copy(query="الحمد"),items).map{it.id})
  assertTrue(filterChapters(base.copy(query="لا يوجد"),items).isEmpty())
 }
}
