/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.manlyit.fitnesscrm.stats.converters;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 *
 * @author dturner
 */
public class ContentCaptureServletResponse
                            extends HttpServletResponseWrapper {
   private ByteArrayOutputStream contentBuffer;
   private PrintWriter writer;
   public ContentCaptureServletResponse(HttpServletResponse resp) {
      super(resp);
   }
   @Override
   public PrintWriter getWriter() throws IOException {
      if(writer == null){
         contentBuffer = new ByteArrayOutputStream();
         writer = new PrintWriter(contentBuffer);
      }
      return writer;
   }
   public String getContent(){
      writer.flush();
      String xhtmlContent = new String(contentBuffer.toByteArray());
      xhtmlContent = xhtmlContent.replaceAll("\\<thead.*?\\>|</thead>","");
      xhtmlContent = xhtmlContent.replaceAll("\\<tbody.*?\\>|</tbody>","");
      //xhtmlContent = xhtmlContent.replaceAll("<thead.>|</thead>|"+"<tbody.>|</tbody>","");
      return xhtmlContent;
   }
}
