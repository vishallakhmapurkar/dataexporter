package com.de.beans;

package com.data.exporter.main

import groovy.util.logging.Log

@Log
class Exporter {
static def props = null

static main(def args){
log.info "Starting Reporting framework...."

log.info "checking for arguments"
if (args.length!=2) {
log.info "Usage: java com.data.exporter.main.Exporter <de.properties> <Report Code>\n"
log.info "For Example: java com.data.exporter.main.Exporter conf/de.properties BrokerCode\n"
System.exit(1)
}
def configfileName =args[0]
def reportCode =args[1]
def pbs1Ins=null
def pbs2Ins=null
try {
log.info "Loading Configuration ...."
def prop =getProps configfileName

def config = new ConfigSlurper().parse(prop)
log.info "Loading DB instances ...."
DBUtil.initializePBSSQLInstance config
log.info "Getting report data for report code $reportCode..."
pbs1Ins=DBUtil.getPBS1Ins()
pbs2Ins=DBUtil.getPBS2Ins()
def reportData=DBUtil.getReportDetails reportCode,pbs2Ins

def finalStr= new StringBuilder()
log.info "Feting data for query $reportData.reportSQl...."
def rs= DBUtil.getResults(reportData.reportSQl, reportData.paramname, true,pbs1Ins,(reportData.isparamrequired.equalsIgnoreCase("Y"))?true:false,reportData.reportheadersname)
def rs2=null

// if(config.usepbs2){
// rs2 = DBUtil.getResult query, config,DBUtil.getPBS2Ins(),false
// }

finalStr.append(rs)

// if(rs2!=null){
// finalStr.append(rs2)
// }

def filename =reportData.reportfilename+DBUtil.getEndDate().replaceAll("/", "-") + Constants.CSV_FILE_EXT
log.info "Generating report file $filename...."
generateReport filename,finalStr
def to = reportData.emailto
def from =reportData.emailfrom
def emailSub=reportData.emailsubjectprefix
emailSub =emailSub.replaceAll("\\}","").replaceAll("\\{","")+DBUtil.getEndDate().replaceAll("/", "-")
def body=reportData.emailbody
body=body.replaceAll("DATE", DBUtil.getEndDate().replaceAll("/", "-"))
log.info "Sending email with attached file $filename...."
Emailer.sendEmail(from,to,emailSub ,body, filename, config.mail.host)
log.info "Email sent successfully ...."

log.info "Ended Reporting framework...."
} catch (Exception e) {
log.info "Reporting framework error "+e.getMessage()
}finally{
if(pbs1Ins!=null && pbs2Ins!=null){
pbs1Ins.close()
pbs2Ins.close()
}
}
}
static void generateReport(filename,finalStr){
def file = new File(filename )
if(file.exists()){
file.delete()
}
new File(filename).withPrintWriter { out ->
out.println "$finalStr"
}
}
static def getProps(def propName){
if(props==null){
props=new Properties()
new File(propName).withInputStream { stream ->
props.load(stream)
}
}
return props
}
}

 

 

package com.data.exporter.main
import groovy.transform.CompileStatic

@CompileStatic
class Constants {
static final def CSV_FILE_EXT = '.csv'
static final def PBS1INS = 'PBS1'
static final def PBS2INS = 'PBS2'
static final def PBSINS = 'PBSInstance'
static final def DATEFORMAT = "MM/dd/yyyy"

static final def REPORTSQL='select reportsql,reportfilename,paramname,isparamrequired,emailto,emailfrom,emailsubjectprefix,emailbody,reportheadersname from TC_Report_Gen where reportname=:rn'

}

 

 

package com.data.exporter.main
import groovy.sql.Sql

import java.text.SimpleDateFormat
class DBUtil {
static def pbs1sqlIns =null
static def pbs2sqlIns =null
public static def SDF = new SimpleDateFormat(Constants.DATEFORMAT);

static def getStartDate(){

def cStartDate= Calendar.getInstance();
def startDate = cStartDate.getTime();
return SDF.format(startDate);
}

static def getEndDate(){
def cEndDate = Calendar.getInstance();

if (cEndDate.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY) {
cEndDate.add(Calendar.DATE, -3);
}
else {
cEndDate.add(Calendar.DATE, -1);
}

def endDate = cEndDate.getTime();

return SDF.format(endDate);
}
static void initializePBSSQLInstance(def config){
if(pbs1sqlIns==null){
def connStr=config.pbs1.TCV.DB.URL + ":" + config.pbs1.TCV.DB.host +":"+ config.pbs1.TCV.DB.port + "/" + config.pbs1.TCV.DB.DBName
pbs1sqlIns = Sql.newInstance(connStr, config.pbs1.TCV.DB.username, config.pbs1.TCV.DB.password,
config.pbs1.TCV.DB.DRIVER)
}
if(config.usepbs2){
def connStr=config.pbs2.TCV.DB.URL + ":" + config.pbs2.TCV.DB.host +":"+ config.pbs2.TCV.DB.port + "/" + config.pbs2.TCV.DB.DBName
pbs2sqlIns = Sql.newInstance(connStr, config.pbs2.TCV.DB.username, config.pbs2.TCV.DB.password,
config.pbs2.TCV.DB.DRIVER)
}
}

static def getPBS1Ins(){
return pbs1sqlIns
}

static def getPBS2Ins(){
return pbs2sqlIns
}

static def getReportDetails(reportName,sqlIns){
def reportdata =new ReportBean()
sqlIns.eachRow(Constants.REPORTSQL,['rn':reportName]){
reportdata.reportSQl = "${it.reportsql}"
reportdata.paramname = "${it.paramname}"
reportdata.emailto ="{$it.emailto}"
reportdata.emailfrom= "${it.emailfrom}"
reportdata.emailsubjectprefix ="{$it.emailsubjectprefix}"
reportdata.emailbody= "${it.emailbody}"
reportdata.isparamrequired="${it.isparamrequired}"
reportdata.reportfilename="${it.reportfilename}"
reportdata.reportheadersname="${it.reportheadersname}"
}
return reportdata
}
static def getResults(def strSql,def params,def isHeaderRequired,def sqlIns,def isparamrequired,def hedrs){

def finalStr =new StringBuilder()
def rowResults=null
def paramsArray=null
if(isparamrequired){
paramsArray= getParamAray(params)

rowResults = sqlIns.rows(strSql.toString(),paramsArray)
}else{
rowResults = sqlIns.rows(strSql.toString())
}
def headers = null
def valus = null
def pbsIns =null

if(isHeaderRequired){
/*headers = rowResults[0].keySet()
headers.eachWithIndex { item, index ->
finalStr.append(item)
finalStr.append(",")
}*/
finalStr.append(hedrs.toString())
finalStr.append(",")
finalStr.append(Constants.PBSINS)
pbsIns = Constants.PBS1INS
}else{
pbsIns = Constants.PBS2INS
}
finalStr.append("\n")
def row=null
def records =new ArrayList<String>()
if(isparamrequired){
sqlIns.eachRow(strSql.toString(),paramsArray){
row="${it}"
records << row.toString()
}
}else{
sqlIns.eachRow(strSql) { row= "${it}" records << row.toString() }
}

for(rw in records){
def str=rw.toString()

def arryRow= str.toString().split(",")
arryRow.each { data->
data=data.toString().replaceAll("\\s","").replaceAll("\\[", "").replaceAll("\\]","");
def dataAry=data.toString().split(":")
if(dataAry.length==2){
def dataval=dataAry[1]
finalStr.append(dataval.toString()+ "\t");
}else{
finalStr.append("NULL"+ "\t");
}
finalStr.append(",");
}
finalStr.append(pbsIns)
finalStr.append("\n")
}

finalStr.append("\n")

return finalStr.toString();
}
static def getParamAray(def params){
def keys=params.toString().replaceAll("\\}","").replaceAll("\\{","").split(",")
def valus = new ArrayList<String>();
keys.eachWithIndex { a, i ->
if(i%2==0){
valus.add(getEndDate())
}
else{

valus.add(getStartDate())
}
}
def vauls = valus.toArray(valus.size())
return zip(keys, vauls)
}
static def zip(keys, values) {
keys.inject([:]) { m, k ->
m[k] = values[m.size()]; m
}
}
}

package com.data.exporter.main

import javax.activation.DataHandler
import javax.activation.FileDataSource
import javax.mail.Message
import javax.mail.MessagingException
import javax.mail.Multipart
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart

class Emailer {
static void sendEmail(def from,def to,def subject,def body,def file,def host) {

def props = new Properties()
props.put("mail.smtp.host", host)
props.put("mail.transport.protocol", "smtp")

def session = Session.getDefaultInstance(props)

def fromAddress = new InternetAddress(from)

def message = new MimeMessage(session)
message.setFrom(fromAddress)
to= to.replaceAll("\\}","").replaceAll("\\{","")
message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to))
message.setSubject(subject)
message.setSentDate(new Date())
MimeBodyPart messagePart = new MimeBodyPart()
messagePart.setText(body)
def fileDataSource = new FileDataSource(file)

def attachmentPart = new MimeBodyPart()
attachmentPart.setDataHandler(new DataHandler(fileDataSource))
attachmentPart.setFileName(fileDataSource.getName())
Multipart multipart = new MimeMultipart()
multipart.addBodyPart(messagePart)
multipart.addBodyPart(attachmentPart)

message.setContent(multipart)

Transport.send(message)

}
}

package com.data.exporter.main