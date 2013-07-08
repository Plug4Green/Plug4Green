package f4g.f4gDeveloperTools.tools.upload;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
 
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
 
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.log4j.Logger;
import f4g.f4gDeveloperTools.tools.modelspy.ModelSpy;
 
 
public class CommonsFileUploadServlet extends HttpServlet {
	static Logger log = Logger.getLogger(CommonsFileUploadServlet.class.getName());
	private static final String TMP_DIR_PATH = "c:\\Temp";
	private File tmpDir;
	private static final String DESTINATION_DIR_PATH ="/files";
	private File destinationDir;
 
	public void init(ServletConfig config) throws ServletException {
		super.init(config); 
	}
 
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	    PrintWriter out = response.getWriter();
	    response.setContentType("text/plain");
	    out.println("<h1>Servlet File Upload Example using Commons File Upload</h1>");
	    out.println();
	    
	    log.debug("inside upload post servlet");
	    
	    
	    
        boolean isMultipart = ServletFileUpload.isMultipartContent(request);
        
        log.debug("request (multipart: " + isMultipart + ") = " + request.getQueryString().toString());

        if (isMultipart) {
            FileItemFactory factory = new DiskFileItemFactory();
            ServletFileUpload upload = new ServletFileUpload(factory);

            try {
                List items = upload.parseRequest(request);
                Iterator iterator = items.iterator();
                while (iterator.hasNext()) {
                    FileItem item = (FileItem) iterator.next();

                    if (!item.isFormField()) {
                        String fileName = item.getName();

                        String root = getServletContext().getRealPath("/");
                        File path = new File(root + "/files");
                        if (!path.exists()) {
                            boolean status = path.mkdirs();
                        }

                        File uploadedFile = new File(path + "/" + fileName);
                        System.out.println(uploadedFile.getAbsolutePath());
                        item.write(uploadedFile);
                    }
                }
            } catch (FileUploadException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}