

import java.io.IOException;
import java.util.ArrayList;
import javax.servlet.ServletException;
import javax.servlet.http.*;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.ODataResponse;
import org.apache.olingo.server.api.ODataHttpHandler;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.edmx.EdmxReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TasksServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(TasksServlet.class);
    
    @Override
    protected void service(final HttpServletRequest req, final HttpServletResponse res)
      throws ServletException, IOException {

      try {
        HttpSession session = req.getSession(true);
        TasksDataProvider dataProvider = (TasksDataProvider) session.getAttribute(TasksDataProvider.class.getName());
        if (dataProvider == null) {
          dataProvider = new TasksDataProvider();
          session.setAttribute(TasksDataProvider.class.getName(), dataProvider);
          LOG.info("Created new data provider.");
        }

        OData odata = OData.newInstance();
        ServiceMetadata edm = odata.createServiceMetadata(
                      new TasksEdmProvider(),
                      new ArrayList<EdmxReference>());
        ODataHttpHandler handler = odata.createHandler(edm);
        handler.register(new TasksProcessor(dataProvider));
        handler.process(req, res);
      } catch (RuntimeException e) {
        LOG.error("Server Error", e);
        throw new ServletException(e);
      }
    }
    
  }