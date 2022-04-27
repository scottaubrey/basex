package org.basex.query.func.fetch;

import static org.basex.query.QueryError.*;

import java.io.*;

import org.basex.build.*;
import org.basex.core.*;
import org.basex.io.*;
import org.basex.query.*;
import org.basex.query.func.*;
import org.basex.query.up.primitives.*;
import org.basex.query.value.item.*;
import org.basex.query.value.node.*;
import org.basex.util.*;
import org.basex.util.options.*;

/**
 * Function implementation.
 *
 * @author BaseX Team 2005-22, BSD License
 * @author Christian Gruen
 */
public class FetchXml extends StandardFunc {
  @Override
  public Item item(final QueryContext qc, final InputInfo ii) throws QueryException {
    return fetch(io(qc), qc);
  }

  /**
   * Parses the input and creates an XML document.
   * @param io input data
   * @param qc query context
   * @return node
   * @throws QueryException query exception
   */
  protected DBNode fetch(final IO io, final QueryContext qc) throws QueryException {
    final Options opts = toOptions(1, new Options(), qc);
    if(!io.exists()) throw FETCH_EXISTS_X.get(info, io);

    final DBOptions dbopts = new DBOptions(opts, MainOptions.PARSING, info);
    final MainOptions mopts = dbopts.assignTo(MainOptions.get());
    try {
      return new DBNode(Parser.singleParser(io, mopts, ""));
    } catch(final IOException ex) {
      throw FETCH_OPEN_X.get(info, ex);
    }
  }

  /**
   * Returns an IO instance.
   * @param qc query context
   * @return return IO instance
   * @throws QueryException query exception
   */
  protected IO io(final QueryContext qc) throws QueryException {
    final byte[] uri = toToken(exprs[0], qc);
    final IO io = IO.get(Token.string(uri));
    if(!io.exists()) throw FETCH_EXISTS_X.get(info, io);
    return io;
  }
}
