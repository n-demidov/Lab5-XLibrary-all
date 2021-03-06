package edu.library.servlets;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;

import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.library.ServletConstants;
import edu.library.domain.GenresDomain;
import edu.library.exceptions.persistence.NoSuchPersistenceException;
import edu.library.exceptions.persistence.PersistException;
import edu.library.exceptions.persistence.ValidException;
import edu.library.persistence.entity.Genre;

@WebServlet(name = "GenreServlet", urlPatterns ={"/genre"})
public class GenreServlet extends AbstractServlet
{
    
    private final String PAGE_TYPE = "type", PAGE_TYPE_ADD = "add";

    @EJB
    private GenresDomain genresDomain;

    /**
     * Handles the HTTP <code>GET</code> method.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        Genre genre = null;
        boolean isAddGenre = true;
        try
        {
            final String pageType = request.getParameter(PAGE_TYPE);

            // Если в параметре запроса указано "add" - то создаём новую книгу, иначе - редактируем
            if (!PAGE_TYPE_ADD.equals(pageType))
            {
                // Во всех остальных случаях - edit
                final Long genreId = Long.parseLong(request.getParameter("id"));
                genre = genresDomain.get(genreId);
                isAddGenre = false;
            }
        } catch (final NumberFormatException | PersistException ex)
        {
            java.util.logging.Logger.getLogger(BookServlet.class.getName()).log(Level.SEVERE, null, ex);
            request.setAttribute("errMsg", ex.getMessage());
        } catch (final NoSuchPersistenceException ex)
        {
            response.sendRedirect(ServletConstants.REDIRECT_GENRES_PAGE);
        }
        forwardToJSP(request, response, genre, isAddGenre);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        final String SAVE_AND_STAY = "save_and_stay",
                SAVE_AND_ADD_ANOTHER = "save_and_add_another",
                DELETE = "delete";
        final String REDIRECT_GENRE_DETAILS = "genre?id=",
                REDIRECT_ADD_GENRE = "genre?type=add";
        
        request.setCharacterEncoding("UTF-8");
        
        Genre genre = null;
        boolean isAddGenre = false;

        try
        {
            final String submitType = request.getParameter("submit_type");
            final String idString = request.getParameter(ServletConstants.GENRE_ID);
            final Long genreId = idString.isEmpty() ? null : Long.parseLong(idString);
            isAddGenre = (genreId == null);

            // Считываем параметры
            genre = new Genre(
                    genreId,
                    request.getParameter(ServletConstants.GENRE_NAME));
            
            // Если пользователь нажал удалить - удаляем книгу по id
            if (DELETE.equals(submitType))
            {
                genresDomain.delete(genreId);
                response.sendRedirect(ServletConstants.REDIRECT_GENRES_PAGE);
                return;
            }

            if (isAddGenre)
            {
                genresDomain.create(genre);
            } else
            {
                genresDomain.update(genre);
            }

            // Перенаправляем польз-ля на нужную ему страницу
            if (SAVE_AND_STAY.equals(submitType))
            {
                // перерисовываем страницу
                if (isAddGenre)
                {
                    response.sendRedirect(REDIRECT_GENRE_DETAILS + genre.getId());
                } else
                {
                    forwardToJSP(request, response, genre, isAddGenre);
                }
            } else if (SAVE_AND_ADD_ANOTHER.equals(submitType))
            {
                response.sendRedirect(REDIRECT_ADD_GENRE);       // редирект на страницу добавления
            } else
            {
                response.sendRedirect(ServletConstants.REDIRECT_GENRES_PAGE);     // редирект на страницу списка книг
            }
        } catch (final NumberFormatException | PersistException | ValidException ex)
        {
            java.util.logging.Logger.getLogger(BookServlet.class.getName()).log(Level.SEVERE, null, ex);
            request.setAttribute("errMsg", ex.getMessage());
            forwardToJSP(request, response, genre, isAddGenre);
        }
    }

    /**
     * Returns a short description of the servlet.
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo()
    {
        return "Short description";
    }
    
    private void forwardToJSP(final HttpServletRequest request, final HttpServletResponse response,
            final Genre genre, final boolean isAddGenre) throws ServletException, IOException
    {
        try
        {
            final List<Genre> genres = genresDomain.getAll();
            
            request.setAttribute("genre", genre);
            request.setAttribute("genres", genres);
            request.setAttribute("isAddGenre", isAddGenre);
        } catch (final PersistException ex)
        {
            java.util.logging.Logger.getLogger(BookServlet.class.getName()).log(Level.SEVERE, null, ex);
            request.setAttribute("errMsg", ex.getMessage());
        }
        
        request.getRequestDispatcher("/genre.jsp").forward(request, response);
    }

}
