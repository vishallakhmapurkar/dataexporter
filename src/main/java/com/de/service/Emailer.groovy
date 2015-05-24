package com.de.service

import javax.activation.DataHandler
import javax.activation.FileDataSource
import javax.mail.Multipart
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart

import org.apache.log4j.Logger
import org.codehaus.groovy.control.messages.Message
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class Emailer implements IEmailer {

	private @Value('${emailhost}') host
	private def log  = Logger.getLogger(IEmailer.class)
	
	public boolean sendEmail(to,from,sub,body,file) {
		def isEmailSent =true
		try{
			log.debug("Sending email with details to:"+to+",from: "+from+",sub: "+sub+",body: "+body+",file: "+file)
			
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
			log.debug("Email sent sucessfully ")
		}catch(all){
		   isEmailSent =false
		   log.error("Email Failure :"+all.message,all)
		}
		return isEmailSent
	}
}
