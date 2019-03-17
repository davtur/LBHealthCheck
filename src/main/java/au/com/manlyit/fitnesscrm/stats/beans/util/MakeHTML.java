/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.beans.util;



import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * Convert a POJO into a simple 2 column HTML table.
 */
public class MakeHTML {
	private static final class HTMLStyle extends ToStringStyle {

        private static final long serialVersionUID = 1L;

		public HTMLStyle() {
			setFieldSeparator("</td></tr>"+ System.lineSeparator() + "<tr><td>");

			setContentStart("<table>"+ System.lineSeparator()+
								"<thead><tr><th>Field</th><th>Data</th></tr></thead>" +
								"<tbody><tr><td>");

			setFieldNameValueSeparator("</td><td>");

			setContentEnd("</td></tr>"+ System.lineSeparator() + "</tbody></table>");

			setArrayContentDetail(false);
			setUseShortClassName(true);
			setUseClassName(false);
			setUseIdentityHashCode(false);
		}

		@Override
		public void appendDetail(StringBuffer buffer, String fieldName, Object value) {
			if (value.getClass().getName().startsWith("java.lang")) {
				super.appendDetail(buffer, fieldName, value);
			} else {
				buffer.append(ReflectionToStringBuilder.toString(value, this));
			}
		}
               
	}

	static public String makeHTML(Object object) {
		return	ReflectionToStringBuilder.toString(object, new HTMLStyle());
	}
}