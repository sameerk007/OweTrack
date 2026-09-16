package com.owetrack.app.export

import android.content.*
import android.graphics.*
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.owetrack.app.data.*
import com.owetrack.app.domain.Money
import com.owetrack.app.ui.components.asDate
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.Instant

object Exporter {
    private fun exportDir(context:Context)=File(context.cacheDir,"exports").apply{mkdirs()}
    private fun share(context:Context,file:File,mime:String,title:String){val uri=FileProvider.getUriForFile(context,"${context.packageName}.files",file);context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply{type=mime;putExtra(Intent.EXTRA_STREAM,uri);addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)},title))}
    fun shareCsv(context:Context,p:PersonSummary,items:List<TransactionEntity>){val f=File(exportDir(context),"${p.person.name.safe()}-ledger.csv");f.bufferedWriter().use{w->w.appendLine("Date,Type,Amount,Method,UPI Provider,Purpose,Notes");items.sortedBy{it.transactionDate}.forEach{t->w.appendLine(listOf(t.transactionDate.asDate("yyyy-MM-dd"),t.type.name,t.amountPaise/100.0,t.paymentMethod.name,t.upiProvider?.name.orEmpty(),t.purpose.orEmpty(),t.notes.orEmpty()).joinToString(","){csv(it.toString())})}};share(context,f,"text/csv","Share ledger")}
    fun sharePdf(context:Context,p:PersonSummary,items:List<TransactionEntity>){val doc=PdfDocument();val pageInfo=PdfDocument.PageInfo.Builder(595,842,1).create();var page=doc.startPage(pageInfo);var canvas=page.canvas;val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=Color.BLACK;textSize=12f};fun line(text:String,bold:Boolean=false){if(canvas.height-50<y){doc.finishPage(page);page=doc.startPage(PdfDocument.PageInfo.Builder(595,842,doc.pages.size+1).create());canvas=page.canvas;y=45f};paint.typeface=if(bold)Typeface.DEFAULT_BOLD else Typeface.DEFAULT;canvas.drawText(text,42f,y,paint);y+=22f};y=48f;paint.textSize=22f;line("${p.person.name} - Lending Statement",true);paint.textSize=12f;if(items.isNotEmpty())line("Period: ${items.minOf{it.transactionDate}.asDate()} - ${items.maxOf{it.transactionDate}.asDate()}");line("Total given: ${Money.format(p.totalGivenPaise)}");line("Total received: ${Money.format(p.totalReceivedPaise)}");line("Outstanding: ${Money.format(p.balancePaise)}",true);y+=10;line("DATE          TYPE          AMOUNT          METHOD / PURPOSE",true);items.sortedBy{it.transactionDate}.forEach{t->line("${t.transactionDate.asDate("dd MMM yy")}   ${t.type.name.padEnd(10)}   ${Money.format(t.amountPaise).padEnd(14)} ${t.paymentMethod.name.replace('_',' ')} ${t.purpose.orEmpty()}")};doc.finishPage(page);val f=File(exportDir(context),"${p.person.name.safe()}-statement.pdf");f.outputStream().use(doc::writeTo);doc.close();share(context,f,"application/pdf","Share statement")}
    private var y=0f
    private fun String.safe()=replace(Regex("[^A-Za-z0-9_-]"),"-")
    private fun csv(v:String)="\"${v.replace("\"","\"\"")}\""
}

object BackupCodec {
    fun encode(people:List<PersonEntity>,transactions:List<TransactionEntity>):String=JSONObject().apply{put("formatVersion",1);put("createdAt",Instant.now().toString());put("people",JSONArray().apply{people.forEach{p->put(JSONObject().apply{put("id",p.personId);put("name",p.name);put("phone",p.phone);put("notes",p.notes);put("created",p.createdAt);put("updated",p.updatedAt)})}});put("transactions",JSONArray().apply{transactions.forEach{t->put(JSONObject().apply{put("id",t.transactionId);put("personId",t.personId);put("type",t.type.name);put("amountPaise",t.amountPaise);put("date",t.transactionDate);put("method",t.paymentMethod.name);put("provider",t.upiProvider?.name);put("purpose",t.purpose);put("notes",t.notes);put("created",t.createdAt);put("updated",t.updatedAt)})}})}.toString(2)
    private fun JSONObject.nullable(key:String)=optString(key,"").takeUnless{it.isBlank()||it=="null"}
    fun decode(raw:String):Pair<List<PersonEntity>,List<TransactionEntity>>{val root=JSONObject(raw);require(root.getInt("formatVersion")==1);val ps=root.getJSONArray("people");val ts=root.getJSONArray("transactions");val people=(0 until ps.length()).map{i->ps.getJSONObject(i).run{PersonEntity(getLong("id"),getString("name"),nullable("phone"),nullable("notes"),getLong("created"),getLong("updated"))}};val ids=people.map{it.personId}.toSet();val tx=(0 until ts.length()).map{i->ts.getJSONObject(i).run{TransactionEntity(getLong("id"),getLong("personId"),TransactionType.valueOf(getString("type")),getLong("amountPaise").also{require(it>0)},getLong("date"),PaymentMethod.valueOf(getString("method")),nullable("provider")?.let(UpiProvider::valueOf),nullable("purpose"),nullable("notes"),getLong("created"),getLong("updated"))}};require(tx.all{it.personId in ids});return people to tx}
}
